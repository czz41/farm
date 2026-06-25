package com.yupi.project.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI常态基准方案主表
 *
 * @TableName irr_ai_plan
 */
@TableName(value = "irr_ai_plan")
@Data
public class IrrAiPlan implements Serializable {
    /**
     * 固定1，唯一AI常态方案
     */
    @TableId(type = IdType.INPUT)
    private Integer id;

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

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
