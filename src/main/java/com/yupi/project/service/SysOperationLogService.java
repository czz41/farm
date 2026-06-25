package com.yupi.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.project.model.entity.SysOperationLog;

/**
 * 操作日志服务
 */
public interface SysOperationLogService extends IService<SysOperationLog> {

    /**
     * 记录一条操作日志
     *
     * @param operationType 操作类型
     * @param content       操作详情
     */
    void log(String operationType, String content);
}
