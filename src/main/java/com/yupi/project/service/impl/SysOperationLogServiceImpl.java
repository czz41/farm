package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.project.mapper.SysOperationLogMapper;
import com.yupi.project.model.entity.SysOperationLog;
import com.yupi.project.service.SysOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 */
@Service
@Slf4j
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
        implements SysOperationLogService {

    @Override
    public void log(String operationType, String content) {
        try {
            SysOperationLog entity = new SysOperationLog();
            entity.setOperationType(operationType);
            entity.setContent(content);
            this.save(entity);
        } catch (Exception e) {
            log.warn("记录操作日志失败 type={} content={}", operationType, content, e);
        }
    }
}
