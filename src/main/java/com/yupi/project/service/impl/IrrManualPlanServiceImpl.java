package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.project.mapper.IrrManualPlanMapper;
import com.yupi.project.model.entity.IrrManualPlan;
import com.yupi.project.service.IrrManualPlanService;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【irr_manual_plan(人工基准方案主表)】的数据库操作Service实现
 */
@Service
public class IrrManualPlanServiceImpl extends ServiceImpl<IrrManualPlanMapper, IrrManualPlan> implements IrrManualPlanService {

}
