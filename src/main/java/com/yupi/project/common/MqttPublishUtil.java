package com.yupi.project.common;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MQTT 消息发布工具（Spring 组件，配置由 application.yml 注入）
 *
 * <pre>
 * ==================== MQTT 主题规划 ====================
 *
 * 【发布主题】（服务器 → 设备）
 *   farm/pi/cmd      下发浇水方案/指令
 *      payload: 纯文本格式，首行 "t"，后续每行 "HH:mm 毫升数"，例如：
 *               t
 *               8:00 50
 *               18:00 100
 *
 * 【订阅主题】（设备 → 服务器）
 *   farm/pi/status   设备心跳/在线状态上报
 *      payload: {"online":true,"timestamp":1719300000000}  或纯文本 "heartbeat"
 *   farm/pi/sensor   设备传感器数据上报
 *      payload: {"soilMoisture":45,"temperature":28,"humidity":60}
 *
 * 【在线判定逻辑】
 *   服务器订阅 status 主题，每次收到消息更新 DeviceStatusHolder.lastSeenTs。
 *   DeviceStatusController 查询时，若 now - lastSeenTs > online-timeout 则判定离线。
 *
 * 【Broker】 tcp://8.134.210.144:1883
 * ======================================================
 * </pre>
 */
@Component
@Slf4j
public class MqttPublishUtil {

    @Value("${mqtt.broker:tcp://8.134.210.144:1883}")
    private String broker;

    @Value("${mqtt.topic:farm/pi/cmd}")
    private String defaultTopic;

    /**
     * 发送 MQTT 消息
     *
     * @param topic   主题
     * @param payload 消息内容字符串
     * @param qos     消息质量 0/1/2
     * @param retained 是否保留消息
     */
    public void sendMsg(String topic, String payload, int qos, boolean retained) {
        MqttClient client = null;
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            String clientId = "java_publish_client_" + System.currentTimeMillis();
            client = new MqttClient(broker, clientId, persistence);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);

            client.connect(options);

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(qos);
            message.setRetained(retained);

            client.publish(topic, message);
            log.info("MQTT发送成功 topic={} payload={}", topic, payload);
        } catch (Exception e) {
            log.error("MQTT发送失败 topic={}", topic, e);
            throw new RuntimeException("MQTT发送失败: " + e.getMessage(), e);
        } finally {
            if (client != null && client.isConnected()) {
                try {
                    client.disconnect();
                } catch (Exception e) {
                    log.warn("MQTT断开连接异常", e);
                }
            }
        }
    }

    /**
     * 使用默认主题发送消息
     */
    public void sendMsg(String payload, int qos, boolean retained) {
        sendMsg(defaultTopic, payload, qos, retained);
    }
}
