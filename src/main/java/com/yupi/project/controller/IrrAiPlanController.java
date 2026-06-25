package com.yupi.project.controller;

import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.irraiplan.IrrAiPlanUpdateRequest;
import com.yupi.project.model.entity.IrrAiPlan;
import com.yupi.project.service.AiPlanService;
import com.yupi.project.service.IrrAiPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * AI常态基准方案接口（固定单条，id恒为1）
 *
 * @author yupi
 */
@RestController
@RequestMapping("/irrAiPlan")
@Slf4j
public class IrrAiPlanController {

    /**
     * 固定主键
     */
    private static final int FIXED_ID = 1;

    @Resource
    private IrrAiPlanService irrAiPlanService;

    @Resource
    private AiPlanService aiPlanService;

    /**
     * 初始化AI常态方案（仅首次使用，id=1）
     *
     * @param irrAiPlanUpdateRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Integer> addIrrAiPlan(@RequestBody IrrAiPlanUpdateRequest irrAiPlanUpdateRequest) {
        if (irrAiPlanUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (irrAiPlanService.getById(FIXED_ID) != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI常态方案已存在，请使用更新接口");
        }
        IrrAiPlan irrAiPlan = new IrrAiPlan();
        BeanUtils.copyProperties(irrAiPlanUpdateRequest, irrAiPlan);
        irrAiPlan.setId(FIXED_ID);
        boolean result = irrAiPlanService.save(irrAiPlan);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(FIXED_ID);
    }

    /**
     * 获取AI常态方案
     *
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<IrrAiPlan> getIrrAiPlan() {
        IrrAiPlan irrAiPlan = irrAiPlanService.getById(FIXED_ID);
        return ResultUtils.success(irrAiPlan);
    }

    /**
     * 更新AI常态方案
     *
     * @param irrAiPlanUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateIrrAiPlan(@RequestBody IrrAiPlanUpdateRequest irrAiPlanUpdateRequest) {
        if (irrAiPlanUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrAiPlan oldPlan = irrAiPlanService.getById(FIXED_ID);
        if (oldPlan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        IrrAiPlan irrAiPlan = new IrrAiPlan();
        BeanUtils.copyProperties(irrAiPlanUpdateRequest, irrAiPlan);
        irrAiPlan.setId(FIXED_ID);
        boolean result = irrAiPlanService.updateById(irrAiPlan);
        return ResultUtils.success(result);
    }

    /**
     * 调用 DeepSeek 生成 AI 浇水方案
     * 读取系统基础配置（植物信息、位置、备注）发送给 AI，
     * 生成后更新唯一 AI 常态方案及其浇水时段明细。
     *
     * @return
     */
    @PostMapping("/generate")
    public BaseResponse<Boolean> generateAiPlan() {
        boolean result = aiPlanService.generateAiPlan();
        return ResultUtils.success(result);
    }
}
