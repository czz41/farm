package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.yupi.project.service.WarnHistoryService;
import com.yupi.project.service.WarnScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

            // 1. 记录新预警 + 邮件通知
            List<QWeatherWarning> newAlerts = new ArrayList<>();
            for (QWeatherWarning w : warnings) {
                if (w.getWarnId() == null || w.getWarnId().isEmpty()) {
                    continue;
                }
                long cnt = warnHistoryService.count(new QueryWrapper<WarnHistory>()
                        .eq("warn_id", w.getWarnId()).eq("msg_type", MSG_ALERT));
                if (cnt == 0) {
                    WarnHistory wh = toWarnHistory(w, MSG_ALERT);
                    warnHistoryService.save(wh);
                    newAlerts.add(w);
                    sendMail(to, "【极端天气预警】" + nullToEmpty(w.getWarnType()),
                            formatWarning("检测到新的极端天气预警", w));
                }
            }

            // 2. 记录已解除的预警（cancel）
            recordCanceledWarnings(currentWarnIds, to);

            // 3. 自动介入：开启介入 + 有新预警 + 当前未处于临时模式
            boolean alreadyInTemp = config.getCurrentPlanType() != null
                    && config.getCurrentPlanType() == PLAN_TYPE_TEMP
                    && getActiveTempPlan() != null;
            if (config.getEnableAutoIntervene() != null && config.getEnableAutoIntervene() == 1
                    && !newAlerts.isEmpty() && !alreadyInTemp) {
                handleIntervene(config, newAlerts.get(0), to);
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

    /**
     * 介入处理：生成临时方案，AI 模式自动切换下发，人工模式邮件提示手动确认
     */
    private void handleIntervene(SysConfig config, QWeatherWarning w, String to) {
        IrrTempPlan temp = new IrrTempPlan();
        // 记录原方案类型，便于结束后恢复
        temp.setSourceType(config.getCurrentPlanType() == null ? PLAN_TYPE_MANUAL : config.getCurrentPlanType());
        temp.setWarnType(w.getWarnType());
        temp.setWarnLevel(w.getWarnLevel());
        temp.setAlertStart(w.getAlertStart());
        temp.setAlertEnd(w.getAlertEnd());
        temp.setDescText(w.getDescText());
        temp.setStatus(1);
        try {
            Long tempId = aiPlanService.generateTempPlan(temp);
            if (config.getCurrentPlanType() != null && config.getCurrentPlanType() == PLAN_TYPE_AI) {
                // AI 模式：自动切换为临时模式并下发
                updatePlanType(config.getId(), PLAN_TYPE_TEMP);
                planPublishService.publishCurrentPlan();
                sendMail(to, "已自动切换临时方案并下发",
                        formatWarning("检测到极端天气，已自动生成临时方案并切换下发。", w)
                                + "\n临时方案ID：" + tempId);
            } else if (config.getCurrentPlanType() != null && config.getCurrentPlanType() == PLAN_TYPE_MANUAL) {
                // 人工模式：邮件提示，等待手动确认
                sendMail(to, "已生成临时方案，请确认是否下发",
                        formatWarning("检测到极端天气，已生成临时方案。请人工确认是否切换下发。", w)
                                + "\n临时方案ID：" + tempId
                                + "\n确认接口：POST /api/irrPlan/activateTemp?tempPlanId=" + tempId);
            }
        } catch (Exception e) {
            log.error("生成临时方案失败", e);
            sendMail(to, "临时方案生成失败",
                    formatWarning("自动介入生成临时方案失败：", w) + "\n错误：" + e.getMessage());
        }
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
            } catch (Exception e) {
                log.warn("恢复后重新下发原方案失败", e);
            }
            sendMail(to, "已恢复原浇水方案",
                    reason + "，已恢复到方案类型：" + planTypeName(sourceType)
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
                        .select("DISTINCT warn_id").eq("msg_type", MSG_ALERT))
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

    private WarnHistory toWarnHistory(QWeatherWarning w, String msgType) {
        WarnHistory wh = new WarnHistory();
        wh.setWarnId(w.getWarnId());
        wh.setWarnType(w.getWarnType());
        wh.setWarnLevel(w.getWarnLevel());
        wh.setAlertStart(w.getAlertStart());
        wh.setAlertEnd(w.getAlertEnd());
        wh.setDescText(w.getDescText());
        wh.setMsgType(msgType);
        return wh;
    }

    private String formatWarning(String prefix, QWeatherWarning w) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder(prefix).append("\n");
        sb.append("预警类型：").append(nullToEmpty(w.getWarnType())).append("\n");
        sb.append("预警等级：").append(nullToEmpty(w.getWarnLevel())).append("\n");
        sb.append("开始时间：").append(w.getAlertStart() == null ? "" : sdf.format(w.getAlertStart())).append("\n");
        sb.append("结束时间：").append(w.getAlertEnd() == null ? "" : sdf.format(w.getAlertEnd())).append("\n");
        sb.append("预警详情：").append(nullToEmpty(w.getDescText()));
        return sb.toString();
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
