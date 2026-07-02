#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
raspberry-pi 灌溉网关完整代码
适配：Arduino Uno + YF-S401霍尔流量计
通信规则：
1. z HH:MM 每秒同步系统时间给Uno
2. t HH:MM ml 下发定时配置（仅存储，不浇水）
3. water xxx 到点下发，启动定量闭环浇水
4. w 开灯蜂鸣 / s 关灯静音
5. Uno上报 water_done target:xx real:xx 上报实际流量
MQTT逻辑完全保留，断线兜底、状态在线、计划上报不变
"""

import os
import re
import json
import time
import logging
import threading
import signal
import paho.mqtt.client as mqtt

# 串口依赖
try:
    import serial
    HAS_SERIAL = True
except ImportError:
    HAS_SERIAL = False
    print("[警告] pyserial未安装，串口将使用模拟模式")

# 日志配置
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

# 全局停止标记 Ctrl+C
stop_flag = threading.Event()

# 捕获中断信号
def sigint_handler(signum, frame):
    logger.info("\n检测到 Ctrl+C，准备停止所有任务...")
    stop_flag.set()

signal.signal(signal.SIGINT, sigint_handler)

# ========== 配置参数 ==========
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, 'data')
os.makedirs(DATA_DIR, exist_ok=True)

MQTT_BROKER = '8.134.210.144'
MQTT_PORT = 1883
MQTT_USERNAME = 'farm'
MQTT_PASSWORD = 'farm123'

TOPIC_CMD = 'farm/pi/cmd'
TOPIC_SENSOR = 'farm/pi/sensor'
TOPIC_STATUS = 'farm/pi/status'

DEVICE_CONFIG_FILE = os.path.join(DATA_DIR, 'device_config.json')
SERIAL_PORT = '/dev/ttyACM0'
SERIAL_BAUD = 9600

# 兜底灌溉计划
FALLBACK_INDOOR = [
    {'time': '08:00', 'amount': 80},
    {'time': '18:00', 'amount': 60},
]
FALLBACK_OUTDOOR = [
    {'time': '06:00', 'amount': 150},
    {'time': '12:00', 'amount': 120},
    {'time': '18:00', 'amount': 100},
]

# 心跳配置
HEARTBEAT_INTERVAL = 30  # 每30秒上报一次在线状态

# ========== 全局状态变量 ==========
current_schedule = []
last_cmd_time = 0
mqtt_connected = False
sensor_thread = None
serial_conn = None
time_sync_thread = None
serial_read_thread = None  # 新增：读取Uno流量反馈线程
last_water_report = time.time()
heartbeat_thread = None  # 新增：心跳线程

# ========== JSON工具函数 ==========
def load_json(filepath):
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            return json.load(f)
    return {}

def save_json(filepath, data):
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

# 加载设备配置
def load_device_config():
    config = load_json(DEVICE_CONFIG_FILE)
    if not config:
        config = {
            'device_id': 'raspberry-pi-01',
            'environment': 'outdoor',
            'enable_fallback': True
        }
        save_json(DEVICE_CONFIG_FILE, config)
    return config

# 初始化串口
def init_serial():
    global serial_conn
    if HAS_SERIAL:
        try:
            serial_conn = serial.Serial(SERIAL_PORT, SERIAL_BAUD, timeout=1)
            logger.info(f"串口连接成功 {SERIAL_PORT}")
            return True
        except Exception as e:
            logger.error(f"串口连接失败: {e}")
            return False
    else:
        logger.info("串口模拟模式已启用")
        return True

# 发送数据到Arduino
def send_to_arduino(content):
    if not content:
        return
    send_text = content if content.endswith('\n') else f"{content}\n"
    logger.info(f"串口下发:\n{send_text.strip()}")
    if HAS_SERIAL and serial_conn and serial_conn.is_open:
        try:
            serial_conn.write(send_text.encode('utf-8'))
        except Exception as e:
            logger.error(f"串口发送失败 {e}")
    else:
        logger.info(f"[模拟] 下发指令: {send_text.strip()}")

# 读取Arduino计划数据（原有上报逻辑）
def read_from_arduino():
    if not (HAS_SERIAL and serial_conn and serial_conn.is_open):
        cfg = load_device_config()
        if cfg.get("environment") == "indoor":
            sim_data = FALLBACK_INDOOR
        else:
            sim_data = FALLBACK_OUTDOOR
        logger.info(f"[模拟串口] 生成计划数据: {sim_data}")
        return sim_data
    try:
        lines = []
        while serial_conn.in_waiting > 0:
            line = serial_conn.readline().decode('utf-8').strip()
            if line:
                lines.append(line.lower())
        if 'plan' in lines:
            plan_index = lines.index('plan')
            plan_data = []
            for line in lines[plan_index+1:]:
                if not line:
                    continue
                match = re.match(r'(\d{1,2}:\d{2})\s+(\d+)', line.strip())
                if match:
                    plan_data.append({
                        'time': match.group(1),
                        'amount': int(match.group(2))
                    })
            if plan_data:
                logger.info(f"解析Uno计划数据: {plan_data}")
                return plan_data
    except Exception as e:
        logger.error(f"串口读取计划异常: {e}")
    return None

# 每秒同步时间 z HH:MM
def time_sync_loop():
    while not stop_flag.is_set():
        try:
            current_time = time.strftime('%H:%M')
            z_cmd = f"z {current_time}"
            send_to_arduino(z_cmd)
            stop_flag.wait(1)
        except Exception as e:
            logger.error(f"时间同步异常: {e}")
            stop_flag.wait(1)

# 上报灌溉计划到MQTT
def sensor_report_loop(client):
    while not stop_flag.is_set():
        plan_data = read_from_arduino()
        if plan_data and mqtt_connected:
            try:
                run_lines = ['run'] + [f"{item['time']} {item['amount']}" for item in plan_data]
                run_payload = '\n'.join(run_lines)
                client.publish(TOPIC_SENSOR, run_payload)
                logger.info(f"MQTT上报灌溉计划:\n{run_payload.strip()}")
            except Exception as e:
                logger.error(f"计划上报MQTT失败: {e}")
        stop_flag.wait(1)

# 新增：串口读取线程，捕获Uno浇水完成反馈
def serial_read_loop(client):
    global last_water_report
    while not stop_flag.is_set():
        if not (HAS_SERIAL and serial_conn and serial_conn.is_open):
            stop_flag.wait(1)
            continue
        try:
            while serial_conn.in_waiting > 0:
                line = serial_conn.readline().decode("utf-8", errors="ignore").strip()
                if not line:
                    continue
                logger.info(f"Uno上传消息: {line}")
                # 捕获流量完成记录并上报MQTT
                if line.startswith("water_done"):
                    mqtt_payload = f"water_record|{line}"
                    client.publish(TOPIC_SENSOR, mqtt_payload)
                    logger.info(f"MQTT上报浇水记录: {mqtt_payload}")
                    last_water_report = time.time()
        except Exception as e:
            logger.error(f"串口读取反馈异常: {e}")
        stop_flag.wait(0.2)

# 新增：MQTT心跳线程，持续上报在线状态
def heartbeat_loop(client):
    while not stop_flag.is_set():
        if mqtt_connected:
            try:
                client.publish(TOPIC_STATUS, 'online', retain=True)
                logger.debug(f"MQTT心跳上报: online")
            except Exception as e:
                logger.error(f"心跳上报失败: {e}")
        # 按配置的间隔等待，支持提前退出
        stop_flag.wait(HEARTBEAT_INTERVAL)

# 定时任务调度核心（修改：到点发送water指令浇水）
def schedule_runner():
    last_executed = set()
    while not stop_flag.is_set():
        if not current_schedule:
            stop_flag.wait(30)
            continue
        now = time.strftime('%H:%M')
        for s in current_schedule:
            schedule_key = f"{s['time']}_{s['amount']}"
            # 到达设定时间，下发浇水指令
            if s['time'] == now and schedule_key not in last_executed:
                target_ml = s['amount']
                logger.info(f"定时触发浇水 {s['time']} 目标水量 {target_ml}ml")
                send_to_arduino(f"water {target_ml}")
                last_executed.add(schedule_key)
        # 清理标记防止内存堆积
        if len(last_executed) > 100:
            last_executed.clear()
        stop_flag.wait(30)

# 加载兜底计划
def load_fallback_schedule():
    config = load_device_config()
    if config.get('environment') == 'indoor':
        return FALLBACK_INDOOR
    else:
        return FALLBACK_OUTDOOR

# MQTT断线兜底逻辑
def check_fallback():
    global current_schedule
    if not mqtt_connected and time.time() - last_cmd_time > 300:
        if not current_schedule:
            fallback = load_fallback_schedule()
            current_schedule = fallback
            logger.warning(f"MQTT离线超5分钟，切换本地兜底计划: {fallback}")
            t_lines = ['t'] + [f"{item['time']} {item['amount']}" for item in fallback]
            send_to_arduino('\n'.join(t_lines))
            return True
    return False

# MQTT连接回调
def on_connect(client, userdata, flags, rc):
    global mqtt_connected
    if rc == 0:
        mqtt_connected = True
        logger.info("MQTT连接成功")
        client.subscribe(TOPIC_CMD)
        logger.info(f"已订阅 {TOPIC_CMD}")
        # 连接成功后立即上报一次在线状态
        client.publish(TOPIC_STATUS, 'online', retain=True)
        logger.info("MQTT首次上报在线状态: online")
    else:
        mqtt_connected = False
        logger.error(f"MQTT连接失败 rc={rc}")

# MQTT断开回调
def on_disconnect(client, userdata, rc):
    global mqtt_connected
    mqtt_connected = False
    logger.warning(f"MQTT断开连接 rc={rc}")

# MQTT消息接收解析
def on_message(client, userdata, msg):
    global current_schedule, last_cmd_time
    try:
        topic = msg.topic
        payload = msg.payload.decode('utf-8').strip()
        logger.info(f"MQTT收到 [{topic}]:\n{payload}")
        if topic == TOPIC_CMD:
            # 原始指令直接透传给Uno(w/s/t全部原样下发)
            send_to_arduino(payload)
            lines = [line.strip().lower() for line in payload.split('\n') if line.strip()]
            # 解析t开头的定时计划更新本地调度列表
            if 't' in lines:
                t_index = lines.index('t')
                schedule = []
                for line in lines[t_index+1:]:
                    match = re.match(r'(\d{1,2}:\d{2})\s+(\d+)', line)
                    if match:
                        schedule.append({
                            'time': match.group(1),
                            'amount': int(match.group(2))
                        })
                if schedule:
                    current_schedule = schedule
                    last_cmd_time = time.time()
                    logger.info(f"灌溉计划已更新: {schedule}")
            else:
                logger.info(f"收到非定时指令 {lines[0]}")
    except Exception as e:
        logger.error(f"MQTT消息处理异常: {e}")

# 主程序入口
def main():
    global sensor_thread, mqtt_connected, serial_conn, time_sync_thread, serial_read_thread, heartbeat_thread
    logger.info("===== 树莓派灌溉网关启动 =====")
    logger.info(f"MQTT服务器 {MQTT_BROKER}:{MQTT_PORT}")
    config = load_device_config()
    logger.info(f"设备配置 {json.dumps(config, ensure_ascii=False)}")

    init_serial()
    fallback = load_fallback_schedule()
    logger.info(f"兜底灌溉计划 {fallback}")

    # 创建MQTT客户端
    client = mqtt.Client(client_id=config.get('device_id', 'raspberry-pi-01'))
    client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)
    client.on_connect = on_connect
    client.on_disconnect = on_disconnect
    client.on_message = on_message
    client.reconnect_delay_set(min_delay=1, max_delay=30)

    # 启动所有后台线程
    sensor_thread = threading.Thread(target=sensor_report_loop, args=(client,), daemon=True)
    sensor_thread.start()

    time_sync_thread = threading.Thread(target=time_sync_loop, daemon=True)
    time_sync_thread.start()

    schedule_thread = threading.Thread(target=schedule_runner, daemon=True)
    schedule_thread.start()

    serial_read_thread = threading.Thread(target=serial_read_loop, args=(client,), daemon=True)
    serial_read_thread.start()

    # 启动心跳线程
    heartbeat_thread = threading.Thread(target=heartbeat_loop, args=(client,), daemon=True)
    heartbeat_thread.start()

    logger.info("所有后台线程启动完成（含心跳线程）")

    # 主循环
    try:
        while not stop_flag.is_set():
            if not mqtt_connected:
                logger.info("尝试重连MQTT...")
                client.connect(MQTT_BROKER, MQTT_PORT, 60)
                client.loop_start()
                time.sleep(3)
            else:
                check_fallback()
            stop_flag.wait(10)
    except Exception as e:
        logger.error(f"主循环异常 {e}")
    finally:
        # 安全释放资源
        logger.info("开始清理资源，断开连接...")
        try:
            client.loop_stop()
            client.disconnect()
            logger.info("MQTT已断开")
        except Exception as e:
            logger.error(f"MQTT关闭异常 {e}")
        if HAS_SERIAL and serial_conn and serial_conn.is_open:
            try:
                serial_conn.close()
                logger.info("串口已关闭")
            except Exception as e:
                logger.error(f"串口关闭异常 {e}")
        logger.info("程序安全退出")

if __name__ == '__main__':
    main()