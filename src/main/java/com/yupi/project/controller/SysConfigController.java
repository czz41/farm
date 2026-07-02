package com.yupi.project.controller;

import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.sysconfig.SysConfigUpdateRequest;
import com.yupi.project.model.dto.sysconfig.SysConfigVO;
import com.yupi.project.model.entity.SysConfig;
import com.yupi.project.model.vo.CityLookupVO;
import com.yupi.project.service.PlanPublishService;
import com.yupi.project.service.QWeatherService;
import com.yupi.project.service.SysConfigService;
import com.yupi.project.service.SysOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 系统全局配置接口（固定单条，id恒为1）
 *
 * @author yupi
 */
@RestController
@RequestMapping("/sysConfig")
@Slf4j
public class SysConfigController {

    /**
     * 固定主键
     */
    private static final int FIXED_ID = 1;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private QWeatherService qWeatherService;

    @Resource
    private PlanPublishService planPublishService;

    @Resource
    private SysOperationLogService operationLogService;

    /**
     * 方案类型：1 人工 / 2 AI / 3 临时
     */
    private static final int PLAN_TYPE_MANUAL = 1;
    private static final int PLAN_TYPE_AI = 2;
    private static final int PLAN_TYPE_TEMP = 3;

    /**
     * 城市名查询：将前端输入的城市名解析为和风城市ID候选列表。
     * 前端可据此做下拉选择，选中后将城市ID或城市名存入 locationCode 均可
     * （后端查询天气预警时会自动解析城市名为城市ID）。
     *
     * @param name 城市名（中文或拼音）
     * @return 候选城市列表
     */
    @GetMapping("/cityLookup")
    public BaseResponse<List<CityLookupVO>> cityLookup(@RequestParam String name) {
        List<CityLookupVO> list = qWeatherService.lookupCity(name);
        return ResultUtils.success(list);
    }

    /**
     * 初始化系统配置（仅首次使用，id=1）
     *
     * @param sysConfigUpdateRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Integer> addSysConfig(@RequestBody SysConfigUpdateRequest sysConfigUpdateRequest) {
        if (sysConfigUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (sysConfigService.getById(FIXED_ID) != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "系统配置已存在，请使用更新接口");
        }
        SysConfig sysConfig = new SysConfig();
        BeanUtils.copyProperties(sysConfigUpdateRequest, sysConfig);
        sysConfig.setId(FIXED_ID);
        sysConfigService.validSysConfig(sysConfig, true);
        boolean result = sysConfigService.save(sysConfig);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(FIXED_ID);
    }

    /**
     * 获取系统配置（locationCode 存城市ID，locationName 存城市名，前端只用 locationName 展示）
     *
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<SysConfigVO> getSysConfig() {
        SysConfig sysConfig = sysConfigService.getById(FIXED_ID);
        if (sysConfig == null) {
            return ResultUtils.success(null);
        }
        SysConfigVO vo = new SysConfigVO();
        BeanUtils.copyProperties(sysConfig, vo);
        return ResultUtils.success(vo);
    }

    /**
     * 更新系统配置
     * 保存后自动下发当前方案（AI 生成由前端在各方案页面显式触发，此处仅下发）。
     * 临时模式不下发（保持临时方案执行中）。
     *
     * @param sysConfigUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSysConfig(@RequestBody SysConfigUpdateRequest sysConfigUpdateRequest) {
        if (sysConfigUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SysConfig oldSysConfig = sysConfigService.getById(FIXED_ID);
        if (oldSysConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        SysConfig sysConfig = new SysConfig();
        BeanUtils.copyProperties(sysConfigUpdateRequest, sysConfig);
        sysConfig.setId(FIXED_ID);
        sysConfigService.validSysConfig(sysConfig, false);
        boolean result = sysConfigService.updateById(sysConfig);

        // 保存成功后自动下发对应方案（临时模式除外）
        Integer planType = sysConfig.getCurrentPlanType();
        if (planType != null && planType != PLAN_TYPE_TEMP) {
            try {
                planPublishService.publishCurrentPlan();
                operationLogService.log("publish", "保存配置触发自动下发方案，类型：" + planTypeName(planType));
            } catch (Exception e) {
                log.warn("保存配置后自动下发方案失败 planType={}", planType, e);
            }
        }
        return ResultUtils.success(result);
    }

    private String planTypeName(int type) {
        switch (type) {
            case PLAN_TYPE_MANUAL:
                return "人工方案";
            case PLAN_TYPE_AI:
                return "AI方案";
            case PLAN_TYPE_TEMP:
                return "临时方案";
            default:
                return String.valueOf(type);
        }
    }
}

