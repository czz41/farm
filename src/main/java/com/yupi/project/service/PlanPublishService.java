package com.yupi.project.service;

/**
 * 浇水方案下发服务
 */
public interface PlanPublishService {

    /**
     * 根据系统基础配置中的当前方案类型（1人工/2AI常态/3极端临时），
     * 取对应方案的浇水时段明细，组装后通过 MQTT 下发到灌溉设备。
     *
     * @return 是否下发成功
     */
    boolean publishCurrentPlan();

    /**
     * 人工模式下手动一键确认切换到指定临时方案：
     * 将系统当前方案类型置为 3（极端临时），下发该临时方案，并发送邮件通知。
     *
     * @param tempPlanId 临时方案主键
     * @return 是否切换下发成功
     */
    boolean activateTempPlan(Long tempPlanId);
}
