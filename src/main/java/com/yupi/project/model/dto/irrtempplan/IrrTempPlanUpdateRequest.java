package com.yupi.project.model.dto.irrtempplan;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 极端天气临时方案更新请求
 *
 * @TableName irr_temp_plan
 */
@Data
public class IrrTempPlanUpdateRequest implements Serializable {
    /**
     * 临时方案主键
     */
    private Long id;

    /**
     * 原基准类型：1人工 2AI，过期切回
     */
    private Integer sourceType;

    /**
     * 预警类型：暴雨/高温/台风/干旱等
     */
    private String warnType;

    /**
     * 预警等级 blue/yellow/orange/red
     */
    private String warnLevel;

    /**
     * 极端天气开始时间
     */
    private Date alertStart;

    /**
     * 极端天气失效时间
     */
    private Date alertEnd;

    /**
     * 1生效中 2已过期作废
     */
    private Integer status;

    /**
     * 植物+预警整合提示词
     */
    private String prompt;

    /**
     * AI临时方案原始JSON
     */
    private String aiResult;

    private static final long serialVersionUID = 1L;
}
