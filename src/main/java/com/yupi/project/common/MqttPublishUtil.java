package com.yupi.project.common;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttPublishUtil {

    // MQTT 连接参数，根据你的服务修改
    private static final String BROKER = "tcp://8.134.210.144:1883";
    private static final String CLIENT_ID = "java_publish_client";
    private static final String USER_NAME = "";
    private static final String PASSWORD = "";

    /**
     * 发送MQTT消息
     * @param topic 主题
     * @param payload 消息内容字符串
     * @param qos 消息质量 0/1/2
     * @param retained 是否保留消息
     */
    public static void sendMsg(String topic, String payload, int qos, boolean retained) {
        MqttClient client = null;
        try {
            // 内存持久化
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(BROKER, CLIENT_ID + System.currentTimeMillis(), persistence);

            // 连接配置
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            if (USER_NAME != null && !USER_NAME.isEmpty()) {
                options.setUserName(USER_NAME);
                options.setPassword(PASSWORD.toCharArray());
            }
            // 连接超时
            options.setConnectionTimeout(10);
            // 心跳
            options.setKeepAliveInterval(30);

            // 建立连接
            client.connect(options);

            // 封装消息
            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(qos);
            message.setRetained(retained);

            // 发布消息
            client.publish(topic, message);
            System.out.println("发送成功，topic: " + topic + " ，内容： " + payload);

        } catch (MqttException e) {
            e.printStackTrace();
        } finally {
            if (client != null && client.isConnected()) {
                try {
                    client.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 测试入口
    public static void main(String[] args) {
        // 示例：给灌溉设备下发浇水指令
        String topic = "irr/device/001/cmd";
        String msg = "{\"waterTime\":10,\"open\":true}";
        // qos0，不保留
        sendMsg(topic, msg, 0, false);
    }
}