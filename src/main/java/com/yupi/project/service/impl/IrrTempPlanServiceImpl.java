package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.mapper.IrrTempPlanMapper;
import com.yupi.project.model.entity.IrrTempPlan;
import com.yupi.project.service.IrrTempPlanService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @description 针对表【irr_temp_plan(极端天气临时方案主表)】的数据库操作Service实现
 */
@Service
public class IrrTempPlanServiceImpl extends ServiceImpl<IrrTempPlanMapper, IrrTempPlan> implements IrrTempPlanService {

    @Override
    public void validIrrTempPlan(IrrTempPlan irrTempPlan, boolean add) {
        if (irrTempPlan == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer sourceType = irrTempPlan.getSourceType();
        String warnType = irrTempPlan.getWarnType();
        String warnLevel = irrTempPlan.getWarnLevel();
        Date alertStart = irrTempPlan.getAlertStart();
        Date alertEnd = irrTempPlan.getAlertEnd();
        Integer status = irrTempPlan.getStatus();
        if (add) {
            if (StringUtils.isAnyBlank(warnType, warnLevel)
                    || ObjectUtils.anyNull(sourceType, alertStart, alertEnd, status)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (sourceType != null && sourceType != 1 && sourceType != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原基准类型不合法");
        }
        if (status != null && status != 1 && status != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态不合法");
        }
        if (alertStart != null && alertEnd != null && alertEnd.before(alertStart)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "失效时间不能早于开始时间");
        }
    }
}
