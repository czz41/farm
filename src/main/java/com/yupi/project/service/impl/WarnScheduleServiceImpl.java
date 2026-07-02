package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.common.MqttPublishUtil;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.dto.warn.WarnSimulateRequest;
import com.yupi.project.model.entity.IrrTempPlan;
import com.yupi.project.model.entity.SysConfig;
import com.yupi.project.model.entity.WarnHistory;
import com.yupi.project.model.vo.QWeatherWarning;
import com.yupi.project.service.AiPlanService;
import com.yupi.project.service.EmailService;
import com.yupi.project.service.IrrTempPlanService;
import com.yupi.project.service.PlanPublishService;
import com.yupi.project.service.QWeatherService;
import com.yupi.project.service.SysConfigService;
import com.yupi.project.service.SysOperationLogService;
import com.yupi.project.service.WarnHistoryService;
import com.yupi.project.service.WarnScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 极端天气预警调度服务实现
 */
@Service
@Slf4j
public class WarnScheduleServiceImpl implements WarnScheduleService {

    /**
     * 方案类型
     */
    private static final int PLAN_TYPE_MANUAL = 1;
    private static final int PLAN_TYPE_AI = 2;
    private static final int PLAN_TYPE_TEMP = 3;

    private static final String MSG_ALERT = "alert";
    private static final String MSG_CANCEL = "cancel";

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private QWeatherService qWeatherService;

    @Resource
    private WarnHistoryService warnHistoryService;

    @Resource
    private EmailService emailService;

    @Resource
    private AiPlanService aiPlanService;

    @Resource
    private IrrTempPlanService irrTempPlanService;

    @Resource
    private PlanPublishService planPublishService;

    @Resource
    private MqttPublishUtil mqttPublishUtil;

    @Resource
    private SysOperationLogService operationLogService;

    /**
     * 每 10 分钟检查一次极端天气
     */
    @Override
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 30 * 1000L)
    public void checkWarnings() {
        try {
            SysConfig config = sysConfigService.getById(1);
            if (config == null) {
                return;
            }
            // 未开启预警则跳过
            if (config.getEnableWarn() == null || config.getEnableWarn() != 1) {
                return;
            }
            String to = config.getMailAddr();
            List<QWeatherWarning> warnings = qWeatherService.queryWarnings(config.getLocationCode());
            Set<String> currentWarnIds = warnings.stream()
                    .map(QWeatherWarning::getWarnId)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(Collectors.toSet());

            // 1. 收集新预警（尚未记录的）
            List<QWeatherWarning> newAlerts = new ArrayList<>();
            for (QWeatherWarning w : warnings) {
                if (w.getWarnId() == null || w.getWarnId().isEmpty()) {
                    continue;
                }
                long cnt = warnHistoryService.count(new QueryWrapper<WarnHistory>()
                        .eq("warn_id", w.getWarnId()).eq("msg_type", MSG_ALERT));
                if (cnt == 0) {
                    newAlerts.add(w);
                }
            }
            // 批量记录新预警（同批次都标记有效，只作废上一批）
            recordAlerts(newAlerts, to);
            // 只要检测到新预警就发送 w 通知设备（不受自动介入开关限制）
            if (!newAlerts.isEmpty()) {
                sendWarnMqtt("w");
            }

            // 2. 记录已解除的预警（cancel）
            recordCanceledWarnings(currentWarnIds, to);

            // 3. 自动介入：开启介入 + 有新预警
            //    若已有生效临时方案，先作废旧方案再生成新的（AI 直接替换下发，人工生成新的供确认）
            if (config.getEnableAutoIntervene() != null && config.getEnableAutoIntervene() == 1
                    && !newAlerts.isEmpty()) {
                dismissActiveTempPlan(config);
                handleIntervene(config, newAlerts, to);
            }

            // 4. 自动恢复：当前无预警且处于临时模式且有生效临时方案
            if (warnings.isEmpty() && config.getCurrentPlanType() != null
                    && config.getCurrentPlanType() == PLAN_TYPE_TEMP) {
                IrrTempPlan activeTemp = getActiveTempPlan();
                if (activeTemp != null) {
                    restoreFromTemp(activeTemp, config, to, "极端天气已结束");
                }
            }
        } catch (Exception e) {
            log.error("定时预警检查异常", e);
        }
    }

    /**
     * 每 10 分钟清理过期临时方案
     */
    @Override
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void restoreExpiredPlans() {
        try {
            SysConfig config = sysConfigService.getById(1);
            if (config == null) {
                return;
            }
            List<IrrTempPlan> expired = irrTempPlanService.list(new QueryWrapper<IrrTempPlan>()
                    .eq("status", 1)
                    .lt("alert_end", new Date()));
            if (expired == null || expired.isEmpty()) {
                return;
            }
            for (IrrTempPlan temp : expired) {
                restoreFromTemp(temp, config, config.getMailAddr(), "临时方案已到期");
            }
        } catch (Exception e) {
            log.error("清理过期临时方案异常", e);
        }
    }

    @Override
    public void simulateWarning(WarnSimulateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SysConfig config = sysConfigService.getById(1);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "系统配置不存在，请先初始化");
        }
        // 构造模拟预警
        QWeatherWarning w = new QWeatherWarning();
        w.setWarnId("SIM-" + System.currentTimeMillis());
        w.setWarnType(request.getWarnType());
        w.setWarnLevel(request.getWarnLevel());
        Date now = new Date();
        w.setAlertStart(now);
        int minutes = (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0)
                ? 60 : request.getDurationMinutes();
        w.setAlertEnd(new Date(now.getTime() + minutes * 60000L));
        // 拼接手动触发备注 + 开始/结束时间
        SimpleDateFormat logSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        logSdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        String manualDesc = "（手动触发）开始：" + logSdf.format(now)
                + "，结束：" + logSdf.format(w.getAlertEnd());
        w.setDescText(nullToEmpty(request.getDescText()) + manualDesc);

        String to = config.getMailAddr();
        // 记录预警 + 邮件
        recordAlerts(Collections.singletonList(w), to);
        // 只要检测到预警就发送 w 通知设备
        sendWarnMqtt("w");
        // 记录操作日志
        operationLogService.log("simulate_warn",
                "手动触发模拟预警：" + request.getWarnType() + " · " + request.getWarnLevel()
                        + " · 持续" + minutes + "分钟" + manualDesc);
        // 自动介入：开启介入即触发，若已有临时方案先作废再生成新的
        if (config.getEnableAutoIntervene() != null && config.getEnableAutoIntervene() == 1) {
            dismissActiveTempPlan(config);
            handleIntervene(config, Collections.singletonList(w), to);
        }
        log.info("测试面板模拟预警完成 warnId={} type={}", w.getWarnId(), w.getWarnType());
    }

    @Override
    public void simulateClear() {
        SysConfig config = sysConfigService.getById(1);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "系统配置不存在，请先初始化");
        }
        if (config.getCurrentPlanType() == null || config.getCurrentPlanType() != PLAN_TYPE_TEMP) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前非临时模式，无需恢复");
        }
        IrrTempPlan activeTemp = getActiveTempPlan();
        if (activeTemp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "无生效中的临时方案");
        }
        restoreFromTemp(activeTemp, config, config.getMailAddr(), "测试面板手动模拟天气恢复");
        operationLogService.log("simulate_clear", "手动模拟天气恢复，临时方案ID：" + activeTemp.getId());
        log.info("测试面板模拟天气恢复完成 tempId={}", activeTemp.getId());
    }

    @Override
    public void manualCancel() {
        SysConfig config = sysConfigService.getById(1);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "系统配置不存在，请先初始化");
        }
        IrrTempPlan activeTemp = getActiveTempPlan();
        if (activeTemp == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "无生效中的临时方案，无需解除");
        }
        // 1. 作废所有有效预警 + 记录一条 cancel 消息到预警历史
        warnHistoryService.update(new UpdateWrapper<WarnHistory>()
                .eq("is_valid", 1).set("is_valid", 0));
        WarnHistory cancel = new WarnHistory();
        cancel.setWarnId("MANUAL-" + System.currentTimeMillis());
        cancel.setWarnType(activeTemp.getWarnType());
        cancel.setWarnLevel(activeTemp.getWarnLevel());
        cancel.setAlertStart(activeTemp.getAlertStart());
        cancel.setAlertEnd(activeTemp.getAlertEnd());
        cancel.setDescText("手动解除预警");
        cancel.setMsgType(MSG_CANCEL);
        cancel.setIsValid(0);
        cancel.setRecordTime(new Date());
        warnHistoryService.save(cancel);
        // 2. 作废临时方案 + 恢复原方案 + 邮件通知
        restoreFromTemp(activeTemp, config, config.getMailAddr(), "手动解除预警");
        // 3. 记录操作日志
        operationLogService.log("manual_cancel", "手动解除预警，作废临时方案ID：" + activeTemp.getId());
        log.info("手动解除预警完成 tempId={}", activeTemp.getId());
    }

    /**
     * 批量记录新预警：先作废上一批有效预警，再保存本批所有预警（同批次都标记有效）
     */
    private void recordAlerts(List<QWeatherWarning> alerts, String to) {
        if (alerts.isEmpty()) {
            return;
        }
        // 作废所有现存有效预警（is_valid=1 -> 0）
        warnHistoryService.update(new UpdateWrapper<WarnHistory>()
                .eq("is_valid", 1).set("is_valid", 0));
        for (QWeatherWarning w : alerts) {
            WarnHistory wh = toWarnHistory(w, MSG_ALERT);
            wh.setIsValid(1);
            warnHistoryService.save(wh);
            sendMail(to, "【极端天气预警】" + nullToEmpty(w.getWarnType()),
                    formatWarning("检测到极端天气预警", w));
        }
    }

    /**
     * 介入处理：合并多条预警信息生成临时方案，AI 模式自动切换下发，人工模式邮件提示手动确认
     */
    private void handleIntervene(SysConfig config, List<QWeatherWarning> alerts, String to) {
        IrrTempPlan temp = new IrrTempPlan();
        // 记录原方案类型，便于结束后恢复
        temp.setSourceType(config.getCurrentPlanType() == null ? PLAN_TYPE_MANUAL : config.getCurrentPlanType());
        // 合并多条预警信息：类型/等级用顿号连接，时间取最早开始最晚结束，详情拼接
        temp.setWarnType(alerts.stream()
                .map(w -> nullToEmpty(w.getWarnType())).filter(s -> !s.isEmpty())
                .collect(Collectors.joining("、")));
        temp.setWarnLevel(alerts.stream()
                .map(w -> nullToEmpty(w.getWarnLevel())).filter(s -> !s.isEmpty())
                .collect(Collectors.joining("、")));
        temp.setAlertStart(alerts.stream().map(QWeatherWarning::getAlertStart)
                .filter(Objects::nonNull).min(Date::compareTo).orElse(null));
        temp.setAlertEnd(alerts.stream().map(QWeatherWarning::getAlertEnd)
                .filter(Objects::nonNull).max(Date::compareTo).orElse(null));
        temp.setDescText(alerts.stream()
                .map(w -> nullToEmpty(w.getDescText())).filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n")));
        temp.setStatus(1);
        String warnSummary = formatWarnings(alerts);
        try {
            Long tempId = aiPlanService.generateTempPlan(temp);
            if (config.getCurrentPlanType() != null && config.getCurrentPlanType() == PLAN_TYPE_AI) {
                // AI 模式：自动切换为临时模式并下发
                updatePlanType(config.getId(), PLAN_TYPE_TEMP);
                planPublishService.publishCurrentPlan();
                sendMail(to, "已自动切换临时方案并下发",
                        "检测到极端天气，已自动生成临时方案并切换下发。\n" + warnSummary
                                + "\n临时方案ID：" + tempId);
            } else if (config.getCurrentPlanType() != null && config.getCurrentPlanType() == PLAN_TYPE_MANUAL) {
                // 人工模式：邮件提示，等待手动确认
                sendMail(to, "已生成临时方案，请确认是否下发",
                        "检测到极端天气，已生成临时方案。请人工确认是否切换下发。\n" + warnSummary
                                + "\n临时方案ID：" + tempId
                                + "\n确认接口：POST /api/irrPlan/activateTemp?tempPlanId=" + tempId);
            }
        } catch (Exception e) {
            log.error("生成临时方案失败", e);
            sendMail(to, "临时方案生成失败",
                    "自动介入生成临时方案失败：\n" + warnSummary + "\n错误：" + e.getMessage());
        }
    }

    /**
     * 作废当前生效的临时方案，切回原方案类型。
     * 用于收到新预警时替换旧临时方案——AI 模式无需中间下发（紧接着 handleIntervene 会下发新临时方案），
     * 人工模式需下发原方案让设备回到人工基准等待确认。
     *
     * @param config 当前配置（内存中 currentPlanType 会被更新为原方案类型）
     * @return true 如果存在并作废了旧临时方案
     */
    private boolean dismissActiveTempPlan(SysConfig config) {
        IrrTempPlan existingTemp = getActiveTempPlan();
        if (existingTemp == null) {
            return false;
        }
        // 作废旧临时方案
        IrrTempPlan upd = new IrrTempPlan();
        upd.setId(existingTemp.getId());
        upd.setStatus(2);
        irrTempPlanService.updateById(upd);
        // 如果当前处于临时模式，切回原方案类型
        if (config.getCurrentPlanType() != null && config.getCurrentPlanType() == PLAN_TYPE_TEMP) {
            Integer sourceType = existingTemp.getSourceType() == null ? PLAN_TYPE_MANUAL : existingTemp.getSourceType();
            updatePlanType(config.getId(), sourceType);
            config.setCurrentPlanType(sourceType);
            // 人工模式需下发原方案让设备回到人工基准；AI 模式跳过（紧接着会生成新临时方案并下发）
            if (sourceType == PLAN_TYPE_MANUAL) {
                try {
                    planPublishService.publishCurrentPlan();
                } catch (Exception e) {
                    log.warn("替换旧临时方案时下发人工方案失败", e);
                }
            }
        }
        log.info("旧临时方案已作废 tempId={}", existingTemp.getId());
        return true;
    }

    /**
     * 恢复到原方案：作废临时方案、还原 current_plan_type、重新下发原方案、邮件通知
     */
    private void restoreFromTemp(IrrTempPlan temp, SysConfig config, String to, String reason) {
        try {
            // 作废临时方案
            IrrTempPlan upd = new IrrTempPlan();
            upd.setId(temp.getId());
            upd.setStatus(2);
            irrTempPlanService.updateById(upd);
            // 还原方案类型
            Integer sourceType = temp.getSourceType() == null ? PLAN_TYPE_MANUAL : temp.getSourceType();
            // 仅当当前仍为临时模式时才还原，避免覆盖用户手动改过的类型
            if (config.getCurrentPlanType() != null && config.getCurrentPlanType() == PLAN_TYPE_TEMP) {
                updatePlanType(config.getId(), sourceType);
            }
            // 重新下发原方案
            try {
                planPublishService.publishCurrentPlan();
                sendWarnMqtt("s");
            } catch (Exception e) {
                log.warn("恢复后重新下发原方案失败", e);
            }
            SimpleDateFormat restoreSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            restoreSdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            sendMail(to, "已恢复原浇水方案",
                    reason + "，已恢复到方案类型：" + planTypeName(sourceType)
                            + "\n恢复时间：" + restoreSdf.format(new Date())
                            + "\n原临时方案ID：" + temp.getId());
            log.info("临时方案已作废并恢复原方案 tempId={} sourceType={}", temp.getId(), sourceType);
        } catch (Exception e) {
            log.error("恢复原方案失败 tempId={}", temp.getId(), e);
        }
    }

    /**
     * 记录已解除的预警（曾经 alert、未 cancel、当前不再返回）
     */
    private void recordCanceledWarnings(Set<String> currentWarnIds, String to) {
        Set<String> alertedIds = warnHistoryService.list(new QueryWrapper<WarnHistory>()
                        .select("DISTINCT warn_id").eq("msg_type", MSG_ALERT).eq("is_valid", 1))
                .stream().map(WarnHistory::getWarnId).filter(id -> id != null).collect(Collectors.toSet());
        Set<String> canceledIds = warnHistoryService.list(new QueryWrapper<WarnHistory>()
                        .select("DISTINCT warn_id").eq("msg_type", MSG_CANCEL))
                .stream().map(WarnHistory::getWarnId).filter(id -> id != null).collect(Collectors.toSet());
        Set<String> activeIds = new HashSet<>(alertedIds);
        activeIds.removeAll(canceledIds);
        for (String id : activeIds) {
            if (!currentWarnIds.contains(id)) {
                // 取原 alert 记录复制字段
                WarnHistory origin = warnHistoryService.getOne(new QueryWrapper<WarnHistory>()
                        .eq("warn_id", id).eq("msg_type", MSG_ALERT).orderByDesc("record_time").last("limit 1"), false);
                // 作废原 alert 记录
                warnHistoryService.update(new UpdateWrapper<WarnHistory>()
                        .eq("warn_id", id).eq("msg_type", MSG_ALERT).eq("is_valid", 1).set("is_valid", 0));
                WarnHistory cancel = new WarnHistory();
                if (origin != null) {
                    cancel.setWarnType(origin.getWarnType());
                    cancel.setWarnLevel(origin.getWarnLevel());
                    cancel.setAlertStart(origin.getAlertStart());
                    cancel.setAlertEnd(origin.getAlertEnd());
                    cancel.setDescText(origin.getDescText());
                }
                cancel.setWarnId(id);
                cancel.setMsgType(MSG_CANCEL);
                cancel.setIsValid(0);
                cancel.setRecordTime(new Date());
                warnHistoryService.save(cancel);
                sendMail(to, "【极端天气解除】" + (origin == null ? "" : nullToEmpty(origin.getWarnType())),
                        "预警已解除。\n预警类型：" + (origin == null ? "" : nullToEmpty(origin.getWarnType()))
                                + "\n预警ID：" + id);
            }
        }
    }

    private void updatePlanType(Integer configId, int planType) {
        SysConfig update = new SysConfig();
        update.setId(configId);
        update.setCurrentPlanType(planType);
        sysConfigService.updateById(update);
    }

    private IrrTempPlan getActiveTempPlan() {
        return irrTempPlanService.getOne(new QueryWrapper<IrrTempPlan>()
                .eq("status", 1).orderByDesc("create_time").last("limit 1"), false);
    }

    /**
     * 发送预警状态 MQTT 通知：w=进入预警模式，s=解除预警恢复安全
     */
    private void sendWarnMqtt(String msg) {
        try {
            mqttPublishUtil.sendMsg(msg, 0, false);
        } catch (Exception e) {
            log.warn("发送MQTT通知失败 msg={}", msg, e);
        }
    }

    private WarnHistory toWarnHistory(QWeatherWarning w, String msgType) {
        WarnHistory wh = new WarnHistory();
        wh.setWarnId(w.getWarnId());
        wh.setWarnType(w.getWarnType());
        wh.setWarnLevel(w.getWarnLevel());
        wh.setAlertStart(w.getAlertStart());
        wh.setAlertEnd(w.getAlertEnd());
        wh.setDescText(w.getDescText());
        wh.setMsgType(msgType);
        wh.setRecordTime(new Date());
        return wh;
    }

    private String formatWarning(String prefix, QWeatherWarning w) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        StringBuilder sb = new StringBuilder(prefix).append("\n");
        sb.append("通知时间：").append(sdf.format(new Date())).append("\n");
        sb.append("预警类型：").append(nullToEmpty(w.getWarnType())).append("\n");
        sb.append("预警等级：").append(nullToEmpty(w.getWarnLevel())).append("\n");
        sb.append("开始时间：").append(w.getAlertStart() == null ? "" : sdf.format(w.getAlertStart())).append("\n");
        sb.append("结束时间：").append(w.getAlertEnd() == null ? "" : sdf.format(w.getAlertEnd())).append("\n");
        sb.append("预警详情：").append(nullToEmpty(w.getDescText()));
        return sb.toString();
    }

    /**
     * 格式化多条预警信息（用于邮件内容）
     */
    private String formatWarnings(List<QWeatherWarning> alerts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < alerts.size(); i++) {
            sb.append(formatWarning("预警" + (i + 1), alerts.get(i))).append("\n");
        }
        return sb.toString().trim();
    }

    private void sendMail(String to, String subject, String content) {
        try {
            emailService.sendMail(to, subject, content);
        } catch (Exception e) {
            log.warn("预警邮件发送失败 subject={}", subject, e);
        }
    }

    private String planTypeName(int type) {
        switch (type) {
            case PLAN_TYPE_MANUAL:
                return "人工方案";
            case PLAN_TYPE_AI:
                return "AI常态方案";
            case PLAN_TYPE_TEMP:
                return "极端临时方案";
            default:
                return String.valueOf(type);
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
