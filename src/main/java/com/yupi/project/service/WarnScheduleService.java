package com.yupi.project.service;

/**
 * 极端天气预警调度服务
 */
public interface WarnScheduleService {

    /**
     * 定时检查极端天气（每 10 分钟）：
     * 预警开启时查询和风天气，记录新预警并邮件通知；
     * 若开启自动介入则生成临时方案，AI 模式自动切换下发，人工模式邮件提示手动确认；
     * 当前无预警且处于临时模式时自动恢复原方案。
     */
    void checkWarnings();

    /**
     * 定时清理过期临时方案（每 10 分钟）：
     * 将已过 alert_end 的生效临时方案作废并恢复原方案类型。
     */
    void restoreExpiredPlans();
}
