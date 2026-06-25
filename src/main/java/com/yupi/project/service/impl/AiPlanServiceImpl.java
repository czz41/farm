package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.entity.IrrAiPlan;
import com.yupi.project.model.entity.IrrPlanItem;
import com.yupi.project.model.entity.IrrTempPlan;
import com.yupi.project.model.entity.SysConfig;
import com.yupi.project.service.AiPlanService;
import com.yupi.project.service.IrrAiPlanService;
import com.yupi.project.service.IrrPlanItemService;
import com.yupi.project.service.IrrTempPlanService;
import com.yupi.project.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AI 浇水方案生成服务实现：调用 DeepSeek 生成方案并落库
 */
@Service
@Slf4j
public class AiPlanServiceImpl implements AiPlanService {

    /**
     * 固定 AI 常态方案主键
     */
    private static final int AI_PLAN_ID = 1;

    /**
     * 方案类型：2 AI常态
     */
    private static final int PARENT_TYPE_AI = 2;

    /**
     * 方案类型：3 极端临时
     */
    private static final int PARENT_TYPE_TEMP = 3;

    private static final String SYSTEM_PROMPT =
            "你是智能灌溉专家。根据给定的植物信息、所处位置与生长阶段备注，制定一份合理的每日浇水方案。" +
                    "只返回纯JSON，不要Markdown、不要解释文字。返回格式严格如下：\n" +
                    "{\"remark\":\"简短的方案说明\",\"items\":[{\"waterTime\":\"HH:mm:ss\",\"waterDuration\":分钟数}]}\n" +
                    "其中 waterTime 为 24 小时制时刻，waterDuration 为单次浇水分钟数（正整数），items 至少 1 条。";

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.timeout:60000}")
    private int timeout;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private IrrAiPlanService irrAiPlanService;

    @Resource
    private IrrPlanItemService irrPlanItemService;

    @Resource
    private IrrTempPlanService irrTempPlanService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean generateAiPlan() {
        // 1. 读取基础配置
        SysConfig config = sysConfigService.getById(1);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "系统配置不存在，请先初始化");
        }
        // 2. 构建提示词
        String prompt = buildPrompt(config);
        // 3. 调用 DeepSeek
        String aiContent = callDeepSeek(prompt);
        // 4. 解析返回
        JsonObject planJson = parsePlanJson(aiContent);
        String remark = planJson.has("remark") && !planJson.get("remark").isJsonNull()
                ? planJson.get("remark").getAsString() : "AI生成浇水方案";
        List<IrrPlanItem> items = parseItems(planJson);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI返回方案未包含有效浇水时段");
        }
        // 5. 更新唯一 AI 常态方案
        IrrAiPlan aiPlan = irrAiPlanService.getById(AI_PLAN_ID);
        boolean isNew = aiPlan == null;
        if (isNew) {
            aiPlan = new IrrAiPlan();
        }
        aiPlan.setId(AI_PLAN_ID);
        aiPlan.setPrompt(prompt);
        aiPlan.setAiResult(aiContent);
        aiPlan.setRemark(remark);
        boolean saved = isNew ? irrAiPlanService.save(aiPlan) : irrAiPlanService.updateById(aiPlan);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI常态方案保存失败");
        }
        // 6. 替换该方案下的浇水时段明细（parent_type=2, parent_id=1）
        QueryWrapper<IrrPlanItem> removeWrapper = new QueryWrapper<>();
        removeWrapper.eq("parent_id", AI_PLAN_ID).eq("parent_type", PARENT_TYPE_AI);
        irrPlanItemService.remove(removeWrapper);
        for (int i = 0; i < items.size(); i++) {
            IrrPlanItem item = items.get(i);
            item.setParentId((long) AI_PLAN_ID);
            item.setParentType(PARENT_TYPE_AI);
            if (item.getSort() == null) {
                item.setSort(i + 1);
            }
            if (item.getEnable() == null) {
                item.setEnable(1);
            }
        }
        boolean itemSaved = irrPlanItemService.saveBatch(items);
        if (!itemSaved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "浇水时段明细保存失败");
        }
        log.info("AI常态方案生成成功，时段数={}", items.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateTempPlan(IrrTempPlan tempPlan) {
        if (tempPlan == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1. 读取基础配置
        SysConfig config = sysConfigService.getById(1);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "系统配置不存在，请先初始化");
        }
        // 2. 构建提示词（极端天气 + 基础信息）
        String prompt = buildTempPrompt(config, tempPlan);
        // 3. 调用 DeepSeek
        String aiContent = callDeepSeek(prompt);
        // 4. 解析返回
        JsonObject planJson = parsePlanJson(aiContent);
        List<IrrPlanItem> items = parseItems(planJson);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI临时方案未包含有效浇水时段");
        }
        // 5. 落库临时方案主表
        tempPlan.setPrompt(prompt);
        tempPlan.setAiResult(aiContent);
        boolean saved = irrTempPlanService.save(tempPlan);
        if (!saved || tempPlan.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "临时方案保存失败");
        }
        // 6. 落库浇水时段明细（parent_type=3, parent_id=tempPlan.id）
        for (int i = 0; i < items.size(); i++) {
            IrrPlanItem item = items.get(i);
            item.setParentId(tempPlan.getId());
            item.setParentType(PARENT_TYPE_TEMP);
            if (item.getSort() == null) {
                item.setSort(i + 1);
            }
            if (item.getEnable() == null) {
                item.setEnable(1);
            }
        }
        boolean itemSaved = irrPlanItemService.saveBatch(items);
        if (!itemSaved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "临时方案明细保存失败");
        }
        log.info("AI临时方案生成成功 id={} 时段数={}", tempPlan.getId(), items.size());
        return tempPlan.getId();
    }

    /**
     * 根据极端天气信息 + 基础配置构建临时方案提示词
     */
    private String buildTempPrompt(SysConfig config, IrrTempPlan tempPlan) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append("【基础信息】\n");
        sb.append("植物名称：").append(nullToEmpty(config.getPlantName())).append("\n");
        sb.append("位置（和风城市ID）：").append(nullToEmpty(config.getLocationCode())).append("\n");
        sb.append("场景：").append(config.getSceneType() != null && config.getSceneType() == 1 ? "室外" : "室内").append("\n");
        sb.append("生长阶段备注：").append(nullToEmpty(config.getSpecialNote())).append("\n");
        sb.append("【极端天气信息】\n");
        sb.append("预警类型：").append(nullToEmpty(tempPlan.getWarnType())).append("\n");
        sb.append("预警等级：").append(nullToEmpty(tempPlan.getWarnLevel())).append("\n");
        sb.append("开始时间：").append(tempPlan.getAlertStart() == null ? "" : sdf.format(tempPlan.getAlertStart())).append("\n");
        sb.append("结束时间：").append(tempPlan.getAlertEnd() == null ? "" : sdf.format(tempPlan.getAlertEnd())).append("\n");
        sb.append("预警详情：").append(nullToEmpty(tempPlan.getDescText())).append("\n");
        sb.append("请结合极端天气对浇水方案做临时调整（如暴雨减少浇水、高温增加浇水频次等），生成一份临时每日浇水方案。");
        return sb.toString();
    }

    /**
     * 根据系统配置构建用户提示词
     */
    private String buildPrompt(SysConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("植物名称：").append(nullToEmpty(config.getPlantName())).append("\n");
        sb.append("位置（和风城市ID）：").append(nullToEmpty(config.getLocationCode())).append("\n");
        sb.append("场景：").append(config.getSceneType() != null && config.getSceneType() == 1 ? "室外" : "室内").append("\n");
        sb.append("生长阶段备注：").append(nullToEmpty(config.getSpecialNote())).append("\n");
        sb.append("请基于以上信息生成每日浇水方案。");
        return sb.toString();
    }

    /**
     * 调用 DeepSeek chat/completions 接口
     */
    private String callDeepSeek(String prompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("sk-your")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未配置 DeepSeek api-key");
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/chat/completions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setDoOutput(true);

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("stream", false);
            JsonArray messages = new JsonArray();
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", SYSTEM_PROMPT);
            messages.add(sys);
            JsonObject user = new JsonObject();
            user.addProperty("role", "user");
            user.addProperty("content", prompt);
            messages.add(user);
            body.add("messages", messages);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String resp = readAll(is);
            if (code < 200 || code >= 300) {
                log.error("DeepSeek调用失败 code={} resp={}", code, resp);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "DeepSeek调用失败: " + code);
            }
            JsonObject respJson = JsonParser.parseString(resp).getAsJsonObject();
            String content = respJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek调用异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "DeepSeek调用异常: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 解析 AI 返回内容为 JSON 对象（兼容 Markdown 代码块包裹）
     */
    private JsonObject parsePlanJson(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI返回内容为空");
        }
        String json = content.trim();
        // 去除可能的 ```json ... ``` 包裹
        if (json.startsWith("```")) {
            json = json.replaceAll("^```\\w*", "").replaceAll("```\\s*$", "").trim();
        }
        // 截取第一个 { 到最后一个 }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI返回内容解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析浇水时段列表
     */
    private List<IrrPlanItem> parseItems(JsonObject planJson) {
        List<IrrPlanItem> items = new ArrayList<>();
        if (!planJson.has("items") || !planJson.get("items").isJsonArray()) {
            return items;
        }
        JsonArray arr = planJson.getAsJsonArray("items");
        for (int i = 0; i < arr.size(); i++) {
            JsonObject o = arr.get(i).getAsJsonObject();
            IrrPlanItem item = new IrrPlanItem();
            String timeStr = o.has("waterTime") ? o.get("waterTime").getAsString() : null;
            Date waterTime = parseTime(timeStr);
            if (waterTime == null) {
                continue;
            }
            item.setWaterTime(waterTime);
            int duration = o.has("waterDuration") ? o.get("waterDuration").getAsInt() : 0;
            if (duration <= 0) {
                continue;
            }
            item.setWaterDuration(duration);
            if (o.has("sort") && !o.get("sort").isJsonNull()) {
                item.setSort(o.get("sort").getAsInt());
            }
            items.add(item);
        }
        return items;
    }

    /**
     * 解析 HH:mm 或 HH:mm:ss 为 Date
     */
    private Date parseTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return null;
        }
        String t = time.trim();
        if (t.length() <= 5) {
            t = t + ":00";
        }
        try {
            return new SimpleDateFormat("HH:mm:ss").parse(t);
        } catch (Exception e) {
            return null;
        }
    }

    private String readAll(InputStream is) {
        if (is == null) {
            return "";
        }
        try {
            byte[] buf = new byte[4096];
            int n;
            StringBuilder sb = new StringBuilder();
            while ((n = is.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
