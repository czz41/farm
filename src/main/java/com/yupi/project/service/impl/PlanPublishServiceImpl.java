package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.MqttPublishUtil;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.entity.IrrPlanItem;
import com.yupi.project.model.entity.IrrTempPlan;
import com.yupi.project.model.entity.SysConfig;
import com.yupi.project.service.EmailService;
import com.yupi.project.service.IrrPlanItemService;
import com.yupi.project.service.IrrTempPlanService;
import com.yupi.project.service.PlanPublishService;
import com.yupi.project.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 浇水方案下发服务实现：按当前方案类型组装并通过 MQTT 下发
 */
@Service
@Slf4j
public class PlanPublishServiceImpl implements PlanPublishService {

    /**
     * 方案类型
     */
    private static final int PLAN_TYPE_MANUAL = 1;
    private static final int PLAN_TYPE_AI = 2;
    private static final int PLAN_TYPE_TEMP = 3;

    /**
     * 人工 / AI 常态方案的固定 parent_id
     */
    private static final long FIXED_PARENT_ID = 1L;

    @Value("${mqtt.topic:irr/device/001/cmd}")
    private String topic;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private IrrPlanItemService irrPlanItemService;

    @Resource
    private IrrTempPlanService irrTempPlanService;

    @Resource
    private EmailService emailService;

    @Override
    public boolean publishCurrentPlan() {
        // 1. 读取当前方案类型
        SysConfig config = sysConfigService.getById(1);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "系统配置不存在，请先初始化");
        }
        Integer planType = config.getCurrentPlanType();
        if (planType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前方案类型未设置");
        }

        long parentId;
        String planName;
        switch (planType) {
            case PLAN_TYPE_MANUAL:
                parentId = FIXED_PARENT_ID;
                planName = "人工方案";
                break;
            case PLAN_TYPE_AI:
                parentId = FIXED_PARENT_ID;
                planName = "AI常态方案";
                break;
            case PLAN_TYPE_TEMP:
                IrrTempPlan tempPlan = getActiveTempPlan();
                if (tempPlan == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "无生效中的极端临时方案");
                }
                parentId = tempPlan.getId();
                planName = "极端临时方案[" + nullToEmpty(tempPlan.getWarnType()) + "]";
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前方案类型不合法: " + planType);
        }
        return publishPlan(planType, parentId, planName);
    }

    @Override
    public boolean activateTempPlan(Long tempPlanId) {
        if (tempPlanId == null || tempPlanId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SysConfig config = sysConfigService.getById(1);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "系统配置不存在，请先初始化");
        }
        IrrTempPlan tempPlan = irrTempPlanService.getById(tempPlanId);
        if (tempPlan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "临时方案不存在");
        }
        if (tempPlan.getStatus() == null || tempPlan.getStatus() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该临时方案已失效，无法切换");
        }
        // 切换当前方案类型为极端临时
        SysConfig update = new SysConfig();
        update.setId(config.getId());
        update.setCurrentPlanType(PLAN_TYPE_TEMP);
        boolean updated = sysConfigService.updateById(update);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "切换方案类型失败");
        }
        // 下发该临时方案
        String planName = "极端临时方案[手动确认][" + nullToEmpty(tempPlan.getWarnType()) + "]";
        boolean published = publishPlan(PLAN_TYPE_TEMP, tempPlan.getId(), planName);
        // 邮件通知
        sendNotifyMail(config.getMailAddr(), "临时方案已手动确认下发",
                "已人工确认切换到临时方案。\n预警类型：" + nullToEmpty(tempPlan.getWarnType())
                        + "\n预警等级：" + nullToEmpty(tempPlan.getWarnLevel())
                        + "\n方案ID：" + tempPlan.getId());
        return published;
    }

    /**
     * 查询指定方案的启用浇水时段，组装 payload 并通过 MQTT 下发
     *
     * @return 是否下发成功
     */
    private boolean publishPlan(int planType, long parentId, String planName) {
        QueryWrapper<IrrPlanItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("parent_id", parentId)
                .eq("parent_type", planType)
                .eq("enable", 1)
                .orderByAsc("sort");
        List<IrrPlanItem> items = irrPlanItemService.list(itemWrapper);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前方案无启用的浇水时段，无法下发");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("planType", planType);
        payload.addProperty("planName", planName);
        payload.addProperty("publishTime", System.currentTimeMillis());
        JsonArray arr = new JsonArray();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        for (int i = 0; i < items.size(); i++) {
            IrrPlanItem item = items.get(i);
            JsonObject o = new JsonObject();
            o.addProperty("waterTime", item.getWaterTime() == null ? "" : sdf.format(item.getWaterTime()));
            o.addProperty("waterDuration", item.getWaterDuration());
            o.addProperty("sort", item.getSort() == null ? i + 1 : item.getSort());
            arr.add(o);
        }
        payload.add("items", arr);

        String msg = payload.toString();
        try {
            MqttPublishUtil.sendMsg(topic, msg, 0, false);
        } catch (Exception e) {
            log.error("MQTT下发失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "MQTT下发失败: " + e.getMessage());
        }
        log.info("方案下发成功 planType={} planName={} 时段数={}", planType, planName, items.size());
        return true;
    }

    /**
     * 获取最新生效中（status=1）的极端临时方案
     */
    private IrrTempPlan getActiveTempPlan() {
        QueryWrapper<IrrTempPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByDesc("create_time").last("limit 1");
        return irrTempPlanService.getOne(wrapper, false);
    }

    private void sendNotifyMail(String to, String subject, String content) {
        try {
            emailService.sendMail(to, subject, content);
        } catch (Exception e) {
            log.warn("下发邮件通知失败 subject={}", subject, e);
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
