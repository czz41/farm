package com.yupi.project.controller;

import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.service.PlanPublishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 浇水方案下发控制器
 *
 * @author yupi
 */
@RestController
@RequestMapping("/irrPlan")
@Slf4j
public class IrrPlanController {

    @Resource
    private PlanPublishService planPublishService;

    /**
     * 下发当前方案（根据 sysConfig.currentPlanType 自动选择人工/AI/临时）
     */
    @PostMapping("/publish")
    public BaseResponse<Boolean> publishCurrentPlan() {
        boolean result = planPublishService.publishCurrentPlan();
        return ResultUtils.success(result);
    }

    /**
     * 人工模式下手动确认切换到指定临时方案并下发
     *
     * @param tempPlanId 临时方案主键
     */
    @PostMapping("/activateTemp")
    public BaseResponse<Boolean> activateTempPlan(@RequestParam Long tempPlanId) {
        if (tempPlanId == null || tempPlanId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = planPublishService.activateTempPlan(tempPlanId);
        return ResultUtils.success(result);
    }
}
