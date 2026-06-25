package com.yupi.project.model.dto.warnhistory;

import com.yupi.project.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 预警记录查询请求
 *
 * @TableName warn_history
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WarnHistoryQueryRequest extends PageRequest implements Serializable {
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
     * alert新增/update更新/cancel解除
     */
    private String msgType;

    private static final long serialVersionUID = 1L;
}
