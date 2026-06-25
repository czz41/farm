package com.yupi.project.model.dto.irraiplan;

import lombok.Data;

import java.io.Serializable;

/**
 * AI常态基准方案更新请求（固定单条，id恒为1）
 *
 * @TableName irr_ai_plan
 */
@Data
public class IrrAiPlanUpdateRequest implements Serializable {
    /**
     * 生成方案使用的提示词
     */
    private String prompt;

    /**
     * AI原始返回JSON
     */
    private String aiResult;

    /**
     * 方案备注
     */
    private String remark;

    private static final long serialVersionUID = 1L;
}
