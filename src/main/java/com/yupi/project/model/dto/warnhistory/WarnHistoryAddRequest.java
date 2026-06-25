package com.yupi.project.model.dto.warnhistory;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 预警记录创建请求
 *
 * @TableName warn_history
 */
@Data
public class WarnHistoryAddRequest implements Serializable {
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
    private Date alertStart;

    /**
     * 灾害结束时间
     */
    private Date alertEnd;

    /**
     * 预警详情描述
     */
    private String descText;

    /**
     * alert新增/update更新/cancel解除
     */
    private String msgType;

    private static final long serialVersionUID = 1L;
}
