package com.yupi.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.DeleteRequest;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.constant.CommonConstant;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.irrtempplan.IrrTempPlanAddRequest;
import com.yupi.project.model.dto.irrtempplan.IrrTempPlanQueryRequest;
import com.yupi.project.model.dto.irrtempplan.IrrTempPlanUpdateRequest;
import com.yupi.project.model.entity.IrrTempPlan;
import com.yupi.project.service.IrrTempPlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 极端天气临时方案接口
 *
 * @author yupi
 */
@RestController
@RequestMapping("/irrTempPlan")
@Slf4j
public class IrrTempPlanController {

    @Resource
    private IrrTempPlanService irrTempPlanService;

    // region 增删改查

    /**
     * 创建
     *
     * @param irrTempPlanAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addIrrTempPlan(@RequestBody IrrTempPlanAddRequest irrTempPlanAddRequest) {
        if (irrTempPlanAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrTempPlan irrTempPlan = new IrrTempPlan();
        BeanUtils.copyProperties(irrTempPlanAddRequest, irrTempPlan);
        irrTempPlanService.validIrrTempPlan(irrTempPlan, true);
        boolean result = irrTempPlanService.save(irrTempPlan);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(irrTempPlan.getId());
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteIrrTempPlan(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        IrrTempPlan oldPlan = irrTempPlanService.getById(id);
        if (oldPlan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean b = irrTempPlanService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param irrTempPlanUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateIrrTempPlan(@RequestBody IrrTempPlanUpdateRequest irrTempPlanUpdateRequest) {
        if (irrTempPlanUpdateRequest == null || irrTempPlanUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrTempPlan irrTempPlan = new IrrTempPlan();
        BeanUtils.copyProperties(irrTempPlanUpdateRequest, irrTempPlan);
        irrTempPlanService.validIrrTempPlan(irrTempPlan, false);
        long id = irrTempPlanUpdateRequest.getId();
        IrrTempPlan oldPlan = irrTempPlanService.getById(id);
        if (oldPlan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean result = irrTempPlanService.updateById(irrTempPlan);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<IrrTempPlan> getIrrTempPlanById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrTempPlan irrTempPlan = irrTempPlanService.getById(id);
        return ResultUtils.success(irrTempPlan);
    }

    /**
     * 获取列表
     *
     * @param irrTempPlanQueryRequest
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<IrrTempPlan>> listIrrTempPlan(IrrTempPlanQueryRequest irrTempPlanQueryRequest) {
        IrrTempPlan irrTempPlanQuery = new IrrTempPlan();
        if (irrTempPlanQueryRequest != null) {
            BeanUtils.copyProperties(irrTempPlanQueryRequest, irrTempPlanQuery);
        }
        QueryWrapper<IrrTempPlan> queryWrapper = new QueryWrapper<>(irrTempPlanQuery);
        queryWrapper.orderByDesc("create_time");
        List<IrrTempPlan> list = irrTempPlanService.list(queryWrapper);
        return ResultUtils.success(list);
    }

    /**
     * 分页获取列表
     *
     * @param irrTempPlanQueryRequest
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<IrrTempPlan>> listIrrTempPlanByPage(IrrTempPlanQueryRequest irrTempPlanQueryRequest) {
        if (irrTempPlanQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrTempPlan irrTempPlanQuery = new IrrTempPlan();
        BeanUtils.copyProperties(irrTempPlanQueryRequest, irrTempPlanQuery);
        long current = irrTempPlanQueryRequest.getCurrent();
        long size = irrTempPlanQueryRequest.getPageSize();
        String sortField = irrTempPlanQueryRequest.getSortField();
        String sortOrder = irrTempPlanQueryRequest.getSortOrder();
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<IrrTempPlan> queryWrapper = new QueryWrapper<>(irrTempPlanQuery);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<IrrTempPlan> page = irrTempPlanService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(page);
    }

    // endregion
}
