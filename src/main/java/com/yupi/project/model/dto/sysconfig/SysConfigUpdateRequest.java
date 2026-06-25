package com.yupi.project.model.dto.sysconfig;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统配置更新请求（固定单条，id恒为1）
 *
 * @TableName sys_config
 */
@Data
public class SysConfigUpdateRequest implements Serializable {
    /**
     * 植物名称
     */
    private String plantName;

    /**
     * 和风城市ID
     */
    private String locationCode;

    /**
     * 生长阶段备注：种子/幼苗期等
     */
    private String specialNote;

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

    private static final long serialVersionUID = 1L;
}
