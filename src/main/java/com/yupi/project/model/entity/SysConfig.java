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
 * 系统全局配置
 *
 * @TableName sys_config
 */
@TableName(value = "sys_config")
@Data
public class SysConfig implements Serializable {
    /**
     * 固定1，仅一条系统配置
     */
    @TableId(type = IdType.INPUT)
    private Integer id;

    /**
     * 植物名称
     */
    private String plantName;

    /**
     * 和风城市ID（存数据库用）
     */
    private String locationCode;

    /**
     * 城市名称（前端展示用，与 locationCode 同时存取）
     */
    private String locationName;

    /**
     * 生长阶段备注：种子/幼苗期等
     */
    private String specialNote;

    /**
     * 种植方式：1花盆盆栽(20-25cm) 2大盆(30cm以上) 3地栽单株
     */
    private Integer plantType;

    /**
     * 预警接收邮箱
     */
    private String mailAddr;

    /**
     * 1室外 2室内
     */
    private Integer sceneType;

    /**
     * 0关闭预警 1开启预警邮件
     */
    private Integer enableWarn;

    /**
     * 0关闭极端自动方案 1开启自动介入
     */
    private Integer enableAutoIntervene;

    /**
     * 1人工方案 2AI常态方案 3极端临时方案
     */
    private Integer currentPlanType;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
