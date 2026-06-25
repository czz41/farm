package com.yupi.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.DeleteRequest;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.constant.CommonConstant;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.warnhistory.WarnHistoryAddRequest;
import com.yupi.project.model.dto.warnhistory.WarnHistoryQueryRequest;
import com.yupi.project.model.entity.WarnHistory;
import com.yupi.project.service.WarnHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 历史预警记录接口（追加写入，不提供更新）
 *
 * @author yupi
 */
@RestController
@RequestMapping("/warnHistory")
@Slf4j
public class WarnHistoryController {

    @Resource
    private WarnHistoryService warnHistoryService;

    // region 增删查

    /**
     * 创建（记录一条预警）
     *
     * @param warnHistoryAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addWarnHistory(@RequestBody WarnHistoryAddRequest warnHistoryAddRequest) {
        if (warnHistoryAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        WarnHistory warnHistory = new WarnHistory();
        BeanUtils.copyProperties(warnHistoryAddRequest, warnHistory);
        warnHistoryService.validWarnHistory(warnHistory, true);
        boolean result = warnHistoryService.save(warnHistory);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(warnHistory.getId());
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteWarnHistory(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        WarnHistory oldHistory = warnHistoryService.getById(id);
        if (oldHistory == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean b = warnHistoryService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<WarnHistory> getWarnHistoryById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        WarnHistory warnHistory = warnHistoryService.getById(id);
        return ResultUtils.success(warnHistory);
    }

    /**
     * 获取列表
     *
     * @param warnHistoryQueryRequest
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<WarnHistory>> listWarnHistory(WarnHistoryQueryRequest warnHistoryQueryRequest) {
        WarnHistory warnHistoryQuery = new WarnHistory();
        if (warnHistoryQueryRequest != null) {
            BeanUtils.copyProperties(warnHistoryQueryRequest, warnHistoryQuery);
        }
        QueryWrapper<WarnHistory> queryWrapper = new QueryWrapper<>(warnHistoryQuery);
        queryWrapper.orderByDesc("record_time");
        List<WarnHistory> list = warnHistoryService.list(queryWrapper);
        return ResultUtils.success(list);
    }

    /**
     * 分页获取列表
     *
     * @param warnHistoryQueryRequest
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<WarnHistory>> listWarnHistoryByPage(WarnHistoryQueryRequest warnHistoryQueryRequest) {
        if (warnHistoryQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        WarnHistory warnHistoryQuery = new WarnHistory();
        BeanUtils.copyProperties(warnHistoryQueryRequest, warnHistoryQuery);
        long current = warnHistoryQueryRequest.getCurrent();
        long size = warnHistoryQueryRequest.getPageSize();
        String sortField = warnHistoryQueryRequest.getSortField();
        String sortOrder = warnHistoryQueryRequest.getSortOrder();
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<WarnHistory> queryWrapper = new QueryWrapper<>(warnHistoryQuery);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<WarnHistory> page = warnHistoryService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(page);
    }

    // endregion
}
