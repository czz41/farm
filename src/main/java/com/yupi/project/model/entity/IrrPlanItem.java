package com.yupi.project.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 存储所有方案的多组浇水时间、水量
 *
 * @TableName irr_plan_item
 */
@TableName(value = "irr_plan_item")
@Data
public class IrrPlanItem implements Serializable {
    /**
     * 浇水时段自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联对应方案的主键ID
     */
    private Long parentId;

    /**
     * 方案类型 1人工/2AI常态/3极端临时
     */
    private Integer parentType;

    /**
     * 每日浇水时刻 HH:mm:00
     */
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    private Date waterTime;

    /**
     * 单次浇水分钟，代表浇水量
     */
    private Integer waterDuration;

    /**
     * 前端展示排序
     */
    private Integer sort;

    /**
     * 该时段是否启用
     */
    private Integer enable;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
