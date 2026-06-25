package com.yupi.project.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 人工基准方案主表
 *
 * @TableName irr_manual_plan
 */
@TableName(value = "irr_manual_plan")
@Data
public class IrrManualPlan implements Serializable {
    /**
     * 固定1，唯一人工方案
     */
    @TableId(type = IdType.INPUT)
    private Integer id;

    /**
     * 方案文字备注
     */
    private String remark;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
