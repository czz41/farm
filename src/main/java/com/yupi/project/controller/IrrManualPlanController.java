package com.yupi.project.controller;

import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.irrmanualplan.IrrManualPlanUpdateRequest;
import com.yupi.project.model.entity.IrrManualPlan;
import com.yupi.project.service.IrrManualPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 人工基准方案接口（固定单条，id恒为1）
 *
 * @author yupi
 */
@RestController
@RequestMapping("/irrManualPlan")
@Slf4j
public class IrrManualPlanController {

    /**
     * 固定主键
     */
    private static final int FIXED_ID = 1;

    @Resource
    private IrrManualPlanService irrManualPlanService;

    /**
     * 初始化人工方案（仅首次使用，id=1）
     *
     * @param irrManualPlanUpdateRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Integer> addIrrManualPlan(@RequestBody IrrManualPlanUpdateRequest irrManualPlanUpdateRequest) {
        if (irrManualPlanUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (irrManualPlanService.getById(FIXED_ID) != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "人工方案已存在，请使用更新接口");
        }
        IrrManualPlan irrManualPlan = new IrrManualPlan();
        BeanUtils.copyProperties(irrManualPlanUpdateRequest, irrManualPlan);
        irrManualPlan.setId(FIXED_ID);
        boolean result = irrManualPlanService.save(irrManualPlan);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(FIXED_ID);
    }

    /**
     * 获取人工方案
     *
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<IrrManualPlan> getIrrManualPlan() {
        IrrManualPlan irrManualPlan = irrManualPlanService.getById(FIXED_ID);
        return ResultUtils.success(irrManualPlan);
    }

    /**
     * 更新人工方案
     *
     * @param irrManualPlanUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateIrrManualPlan(@RequestBody IrrManualPlanUpdateRequest irrManualPlanUpdateRequest) {
        if (irrManualPlanUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrManualPlan oldPlan = irrManualPlanService.getById(FIXED_ID);
        if (oldPlan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        IrrManualPlan irrManualPlan = new IrrManualPlan();
        BeanUtils.copyProperties(irrManualPlanUpdateRequest, irrManualPlan);
        irrManualPlan.setId(FIXED_ID);
        boolean result = irrManualPlanService.updateById(irrManualPlan);
        return ResultUtils.success(result);
    }
}
