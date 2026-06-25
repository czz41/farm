package com.yupi.project.controller;

import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.warn.WarnSimulateRequest;
import com.yupi.project.service.WarnScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 预警测试面板接口
 */
@RestController
@RequestMapping("/warn")
@Slf4j
public class WarnTestController {

    @Resource
    private WarnScheduleService warnScheduleService;

    /**
     * 模拟收到一条极端天气预警，触发真实预警流程（记录、邮件、按配置介入）
     *
     * @param request 模拟预警参数
     * @return
     */
    @PostMapping("/simulate")
    public BaseResponse<Boolean> simulateWarning(@RequestBody WarnSimulateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        warnScheduleService.simulateWarning(request);
        return ResultUtils.success(true);
    }

    /**
     * 模拟极端天气结束，触发恢复原方案
     *
     * @return
     */
    @PostMapping("/simulateClear")
    public BaseResponse<Boolean> simulateClear() {
        warnScheduleService.simulateClear();
        return ResultUtils.success(true);
    }

    /**
     * 手动解除当前预警：作废生效中的临时方案、恢复原方案、记录 cancel、邮件通知。
     * 供预警历史页面 / 临时方案页面调用。
     *
     * @return
     */
    @PostMapping("/manualCancel")
    public BaseResponse<Boolean> manualCancel() {
        warnScheduleService.manualCancel();
        return ResultUtils.success(true);
    }
}
