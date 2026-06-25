package com.yupi.project.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 和风天气预警信息（解析后内部使用）
 */
@Data
public class QWeatherWarning implements Serializable {
    /**
     * 预警唯一标识
     */
    private String warnId;

    /**
     * 预警类型（如 暴雨/高温/台风）
     */
    private String warnType;

    /**
     * 预警等级
     */
    private String warnLevel;

    /**
     * 预警标题
     */
    private String title;

    /**
     * 灾害开始时间
     */
    private Date alertStart;

    /**
     * 灾害结束时间
     */
    private Date alertEnd;

    /**
     * 预警详情描述
     */
    private String descText;

    private static final long serialVersionUID = 1L;
}
