package com.yupi.project.service;

import com.yupi.project.model.entity.IrrTempPlan;

/**
 * AI 浇水方案生成服务
 */
public interface AiPlanService {

    /**
     * 读取系统基础配置（植物信息、位置、备注）调用 DeepSeek 生成浇水方案，
     * 更新唯一 AI 常态方案（irr_ai_plan id=1）及其浇水时段明细。
     *
     * @return 是否生成成功
     */
    boolean generateAiPlan();

    /**
     * 根据极端天气信息 + 基础配置调用 DeepSeek 生成临时浇水方案。
     * 调用前 tempPlan 已包含天气字段（warnType/warnLevel/alertStart/alertEnd/sourceType/status），
     * 调用后填充 prompt/aiResult 并落库，同时写入浇水时段明细（parent_type=3）。
     *
     * @param tempPlan 极端天气临时方案（未落库）
     * @return 落库后的临时方案主键
     */
    Long generateTempPlan(IrrTempPlan tempPlan);
}
