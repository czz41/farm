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
 * 历史预警记录表
 *
 * @TableName warn_history
 */
@TableName(value = "warn_history")
@Data
public class WarnHistory implements Serializable {
    /**
     * 预警记录主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 和风预警唯一标识
     */
    private String warnId;

    /**
     * 预警类型
     */
    private String warnType;

    /**
     * 预警等级
     */
    private String warnLevel;

    /**
     * 灾害开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date alertStart;

    /**
     * 灾害结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date alertEnd;

    /**
     * 预警详情描述
     */
    private String descText;

    /**
     * alert新增/update更新/cancel解除
     */
    private String msgType;

    /**
     * 是否有效：1有效 0已作废
     */
    private Integer isValid;

    /**
     * 记录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date recordTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
