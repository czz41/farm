package com.yupi.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.project.model.entity.IrrTempPlan;

/**
 * @description 针对表【irr_temp_plan(极端天气临时方案主表)】的数据库操作Service
 */
public interface IrrTempPlanService extends IService<IrrTempPlan> {

    /**
     * 校验
     *
     * @param irrTempPlan
     * @param add 是否为创建校验
     */
    void validIrrTempPlan(IrrTempPlan irrTempPlan, boolean add);
}
