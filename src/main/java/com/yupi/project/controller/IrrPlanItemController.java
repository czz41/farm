package com.yupi.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.DeleteRequest;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.constant.CommonConstant;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.irrplanitem.IrrPlanItemAddRequest;
import com.yupi.project.model.dto.irrplanitem.IrrPlanItemQueryRequest;
import com.yupi.project.model.dto.irrplanitem.IrrPlanItemUpdateRequest;
import com.yupi.project.model.entity.IrrPlanItem;
import com.yupi.project.service.IrrPlanItemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 浇水时段接口（所有方案的浇水时间、水量明细）
 *
 * @author yupi
 */
@RestController
@RequestMapping("/irrPlanItem")
@Slf4j
public class IrrPlanItemController {

    @Resource
    private IrrPlanItemService irrPlanItemService;

    // region 增删改查

    /**
     * 创建
     *
     * @param irrPlanItemAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addIrrPlanItem(@RequestBody IrrPlanItemAddRequest irrPlanItemAddRequest) {
        if (irrPlanItemAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrPlanItem irrPlanItem = new IrrPlanItem();
        BeanUtils.copyProperties(irrPlanItemAddRequest, irrPlanItem);
        irrPlanItemService.validIrrPlanItem(irrPlanItem, true);
        boolean result = irrPlanItemService.save(irrPlanItem);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(irrPlanItem.getId());
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteIrrPlanItem(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        IrrPlanItem oldItem = irrPlanItemService.getById(id);
        if (oldItem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean b = irrPlanItemService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param irrPlanItemUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateIrrPlanItem(@RequestBody IrrPlanItemUpdateRequest irrPlanItemUpdateRequest) {
        if (irrPlanItemUpdateRequest == null || irrPlanItemUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrPlanItem irrPlanItem = new IrrPlanItem();
        BeanUtils.copyProperties(irrPlanItemUpdateRequest, irrPlanItem);
        irrPlanItemService.validIrrPlanItem(irrPlanItem, false);
        long id = irrPlanItemUpdateRequest.getId();
        IrrPlanItem oldItem = irrPlanItemService.getById(id);
        if (oldItem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean result = irrPlanItemService.updateById(irrPlanItem);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<IrrPlanItem> getIrrPlanItemById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrPlanItem irrPlanItem = irrPlanItemService.getById(id);
        return ResultUtils.success(irrPlanItem);
    }

    /**
     * 获取列表（可按 parent_id + parent_type 过滤）
     *
     * @param irrPlanItemQueryRequest
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<IrrPlanItem>> listIrrPlanItem(IrrPlanItemQueryRequest irrPlanItemQueryRequest) {
        IrrPlanItem irrPlanItemQuery = new IrrPlanItem();
        if (irrPlanItemQueryRequest != null) {
            BeanUtils.copyProperties(irrPlanItemQueryRequest, irrPlanItemQuery);
        }
        QueryWrapper<IrrPlanItem> queryWrapper = new QueryWrapper<>(irrPlanItemQuery);
        queryWrapper.orderByAsc("sort");
        List<IrrPlanItem> list = irrPlanItemService.list(queryWrapper);
        return ResultUtils.success(list);
    }

    /**
     * 分页获取列表
     *
     * @param irrPlanItemQueryRequest
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<IrrPlanItem>> listIrrPlanItemByPage(IrrPlanItemQueryRequest irrPlanItemQueryRequest) {
        if (irrPlanItemQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        IrrPlanItem irrPlanItemQuery = new IrrPlanItem();
        BeanUtils.copyProperties(irrPlanItemQueryRequest, irrPlanItemQuery);
        long current = irrPlanItemQueryRequest.getCurrent();
        long size = irrPlanItemQueryRequest.getPageSize();
        String sortField = irrPlanItemQueryRequest.getSortField();
        String sortOrder = irrPlanItemQueryRequest.getSortOrder();
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<IrrPlanItem> queryWrapper = new QueryWrapper<>(irrPlanItemQuery);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<IrrPlanItem> page = irrPlanItemService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(page);
    }

    // endregion
}
