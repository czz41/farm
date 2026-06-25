package com.yupi.project.service;

import com.yupi.project.model.dto.warn.WarnSimulateRequest;

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

    /**
     * 测试面板：模拟收到一条极端天气预警。
     * 直接走真实预警流程（记录 warn_history、发邮件、按配置决定是否自动介入），
     * 用于在不依赖真实天气的情况下测试整套预警逻辑。
     *
     * @param request 模拟预警参数
     */
    void simulateWarning(WarnSimulateRequest request);

    /**
     * 测试面板：模拟极端天气结束，触发恢复原方案。
     * 仅当当前处于临时模式且有生效临时方案时生效。
     */
    void simulateClear();

    /**
     * 手动解除当前预警：作废生效中的临时方案、恢复原方案、记录 cancel 消息、邮件通知。
     * 用于预警历史页面 / 临时方案页面的人工解除操作。
     */
    void manualCancel();
}
