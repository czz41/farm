package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.mapper.IrrPlanItemMapper;
import com.yupi.project.model.entity.IrrPlanItem;
import com.yupi.project.service.IrrPlanItemService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @description 针对表【irr_plan_item(存储所有方案的多组浇水时间、水量)】的数据库操作Service实现
 */
@Service
public class IrrPlanItemServiceImpl extends ServiceImpl<IrrPlanItemMapper, IrrPlanItem> implements IrrPlanItemService {

    @Override
    public void validIrrPlanItem(IrrPlanItem irrPlanItem, boolean add) {
        if (irrPlanItem == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long parentId = irrPlanItem.getParentId();
        Integer parentType = irrPlanItem.getParentType();
        Date waterTime = irrPlanItem.getWaterTime();
        Integer waterDuration = irrPlanItem.getWaterDuration();
        if (add) {
            if (ObjectUtils.anyNull(parentId, parentType, waterTime, waterDuration)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (parentType != null && (parentType < 1 || parentType > 3)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "方案类型不合法");
        }
        if (waterDuration != null && waterDuration <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "浇水时长需大于0");
        }
    }
}
