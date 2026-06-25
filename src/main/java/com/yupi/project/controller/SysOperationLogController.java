package com.yupi.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.model.entity.SysOperationLog;
import com.yupi.project.service.SysOperationLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 操作日志接口
 */
@RestController
@RequestMapping("/sysOperationLog")
public class SysOperationLogController {

    @Resource
    private SysOperationLogService sysOperationLogService;

    /**
     * 获取最近的操作日志（最多50条）
     */
    @GetMapping("/list")
    public BaseResponse<List<SysOperationLog>> list() {
        QueryWrapper<SysOperationLog> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time").last("limit 50");
        List<SysOperationLog> list = sysOperationLogService.list(wrapper);
        return ResultUtils.success(list);
    }
}
