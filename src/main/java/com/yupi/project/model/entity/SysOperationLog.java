package com.yupi.project.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 操作日志表
 */
@TableName(value = "sys_operation_log")
@Data
public class SysOperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作类型：simulate_warn/simulate_clear/publish/activate_temp
     */
    private String operationType;

    /**
     * 操作详情
     */
    private String content;

    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
