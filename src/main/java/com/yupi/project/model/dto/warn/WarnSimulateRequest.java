package com.yupi.project.model.dto.warn;

import lombok.Data;

import java.io.Serializable;

/**
 * 测试面板：模拟极端天气预警请求
 */
@Data
public class WarnSimulateRequest implements Serializable {

    /**
     * 预警类型（暴雨/高温/台风/干旱等）
     */
    private String warnType;

    /**
     * 预警等级（blue/yellow/orange/red 或 蓝色/黄色/橙色/红色）
     */
    private String warnLevel;

    /**
     * 预警持续时长（分钟），用于推算 alert_end
     */
    private Integer durationMinutes;

    /**
     * 预警详情描述
     */
    private String descText;

    private static final long serialVersionUID = 1L;
}
