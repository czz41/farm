package com.yupi.project.model.dto.irrmanualplan;

import lombok.Data;

import java.io.Serializable;

/**
 * 人工基准方案更新请求（固定单条，id恒为1）
 *
 * @TableName irr_manual_plan
 */
@Data
public class IrrManualPlanUpdateRequest implements Serializable {
    /**
     * 方案文字备注
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}
