package com.yupi.project.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备在线状态
 */
@Data
public class DeviceStatusVO implements Serializable {

    /**
     * 是否在线
     */
    private Boolean online;

    /**
     * 最近一次心跳时间戳（毫秒）
     */
    private Long lastSeen;

    /**
     * 最近一次上报内容
     */
    private String lastPayload;

    private static final long serialVersionUID = 1L;
}
