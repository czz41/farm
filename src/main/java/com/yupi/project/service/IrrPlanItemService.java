package com.yupi.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.project.model.entity.IrrPlanItem;

/**
 * @description 针对表【irr_plan_item(存储所有方案的多组浇水时间、水量)】的数据库操作Service
 */
public interface IrrPlanItemService extends IService<IrrPlanItem> {

    /**
     * 校验
     *
     * @param irrPlanItem
     * @param add 是否为创建校验
     */
    void validIrrPlanItem(IrrPlanItem irrPlanItem, boolean add);
}
