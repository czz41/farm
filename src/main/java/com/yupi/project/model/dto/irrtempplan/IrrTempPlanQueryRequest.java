package com.yupi.project.model.dto.irrtempplan;

import com.yupi.project.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 极端天气临时方案查询请求
 *
 * @TableName irr_temp_plan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class IrrTempPlanQueryRequest extends PageRequest implements Serializable {
    /**
     * 原基准类型：1人工 2AI
     */
    private Integer sourceType;

    /**
     * 预警类型
     */
    private String warnType;

    /**
     * 预警等级
     */
    private String warnLevel;

    /**
     * 1生效中 2已过期作废
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
