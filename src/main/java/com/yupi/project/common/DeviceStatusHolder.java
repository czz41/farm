package com.yupi.project.common;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 设备在线状态持有者：记录最近一次心跳时间与上报内容。
 * 由 MqttSubscriber 收到设备状态消息时更新。
 */
@Component
public class DeviceStatusHolder {

    /**
     * 最近一次心跳时间戳（毫秒），0 表示从未收到
     */
    private final AtomicLong lastSeenTs = new AtomicLong(0L);

    /**
     * 最近一次上报的原始内容
     */
    private volatile String lastPayload = "";

    public void update(String payload) {
        lastSeenTs.set(System.currentTimeMillis());
        if (payload != null) {
            lastPayload = payload;
        }
    }

    public long getLastSeenTs() {
        return lastSeenTs.get();
    }

    public String getLastPayload() {
        return lastPayload;
    }
}
