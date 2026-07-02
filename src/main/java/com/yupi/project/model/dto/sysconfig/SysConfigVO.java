package com.yupi.project.model.dto.sysconfig;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统配置返回VO（用于 getSysConfig 接口）
 * 相比 UpdateRequest 多带一个 locationName（城市名）供前端直接展示，
 * 解决前端只拿到 locationCode 城市ID 时显示不友好的问题。
 */
@Data
public class SysConfigVO implements Serializable {

    /**
     * 固定1
     */
    private Integer id;

    /**
     * 植物名称
     */
    private String plantName;

    /**
     * 和风城市ID（用于后端查询天气）
     */
    private String locationCode;

    /**
     * 城市名（前端展示用）
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
    private java.util.Date updateTime;

    private static final long serialVersionUID = 1L;
}
