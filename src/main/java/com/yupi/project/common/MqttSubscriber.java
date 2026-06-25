package com.yupi.project.common;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * MQTT 订阅者：监听设备状态主题（心跳），更新设备在线状态。
 * 启动时连接并订阅，断线自动重连并由 connectComplete 重新订阅。
 *
 * 订阅主题：
 *   farm/pi/status — 设备心跳/在线状态上报，收到即更新 DeviceStatusHolder
 *   farm/pi/sensor — 传感器数据上报
 */
@Component
@Slf4j
public class MqttSubscriber {

    @Value("${mqtt.broker:tcp://8.134.210.144:1883}")
    private String broker;

    @Value("${mqtt.status-topic:farm/pi/status}")
    private String statusTopic;

    @Value("${mqtt.data-topic:farm/pi/sensor}")
    private String dataTopic;

    @Resource
    private DeviceStatusHolder deviceStatusHolder;

    private MqttClient client;

    @PostConstruct
    public void init() {
        connect();
    }

    /**
     * 定时检查连接，断开则重连
     */
    @Scheduled(fixedDelay = 30 * 1000L, initialDelay = 30 * 1000L)
    public void reconnect() {
        try {
            if (client == null || !client.isConnected()) {
                log.info("MQTT未连接，尝试重连...");
                connect();
            }
        } catch (Exception e) {
            log.warn("MQTT重连失败", e);
        }
    }

    private void connect() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            String clientId = "java_subscribe_client_" + System.currentTimeMillis();
            client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            options.setAutomaticReconnect(true);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    // 重连后需重新订阅
                    if (reconnect) {
                        try {
                            client.subscribe(statusTopic, 0);
                            client.subscribe(dataTopic, 0);
                            log.info("MQTT重连后重新订阅成功 statusTopic={} dataTopic={}", statusTopic, dataTopic);
                        } catch (Exception e) {
                            log.warn("MQTT重连后订阅失败", e);
                        }
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT连接丢失", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    if (statusTopic.equals(topic)) {
                        // 设备心跳/状态上报 → 更新在线状态
                        deviceStatusHolder.update(payload);
                        log.info("收到设备状态 topic={} payload={}", topic, payload);
                    } else if (dataTopic.equals(topic)) {
                        // 传感器数据上报（预留扩展）
                        log.info("收到设备数据 topic={} payload={}", topic, payload);
                    } else {
                        log.info("收到未知主题消息 topic={} payload={}", topic, payload);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.connect(options);
            client.subscribe(statusTopic, 0);
            client.subscribe(dataTopic, 0);
            log.info("MQTT订阅成功 broker={} statusTopic={} dataTopic={}", broker, statusTopic, dataTopic);
        } catch (Exception e) {
            log.error("MQTT订阅初始化失败 broker={}", broker, e);
        }
    }
}
