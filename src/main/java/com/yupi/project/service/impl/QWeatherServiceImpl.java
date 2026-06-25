package com.yupi.project.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.model.vo.CityLookupVO;
import com.yupi.project.model.vo.QWeatherWarning;
import com.yupi.project.service.QWeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 和风天气预警查询服务实现
 * 接口：GET {base-url}/v7/warning/now?location={cityId}&key={apiKey}
 */
@Service
@Slf4j
public class QWeatherServiceImpl implements QWeatherService {

    @Value("${qweather.api-key:}")
    private String apiKey;

    @Value("${qweather.base-url:https://api.qweather.com}")
    private String baseUrl;

    /**
     * 城市查询（GeoAPI）固定走独立域名
     */
    private static final String GEO_BASE_URL = "https://geoapi.qweather.com";

    private static final String TIME_PATTERN = "yyyy-MM-dd'T'HH:mmXXX";

    /**
     * 城市名 -> 城市ID 缓存（城市ID不变，可长期缓存，避免每次轮询重复查询）
     */
    private final Map<String, String> cityCodeCache = new ConcurrentHashMap<>();

    @Override
    public List<QWeatherWarning> queryWarnings(String locationCodeOrName) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("your-qweather")) {
            log.warn("未配置和风天气 api-key，跳过预警查询");
            return Collections.emptyList();
        }
        // 城市名/城市ID 统一解析为和风城市ID
        String locationId = resolveLocationId(locationCodeOrName);
        if (locationId == null || locationId.isEmpty()) {
            log.warn("无法解析城市：{}", locationCodeOrName);
            return Collections.emptyList();
        }
        HttpURLConnection conn = null;
        try {
            String urlStr = baseUrl + "/v7/warning/now?location="
                    + URLEncoder.encode(locationId, "UTF-8")
                    + "&key=" + URLEncoder.encode(apiKey, "UTF-8");
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String resp = readAll(is);
            if (code < 200 || code >= 300) {
                log.error("和风天气查询失败 code={} resp={}", code, resp);
                return Collections.emptyList();
            }
            JsonObject respJson = JsonParser.parseString(resp).getAsJsonObject();
            String codeStr = respJson.has("code") ? respJson.get("code").getAsString() : "";
            // 200 表示有数据；204 表示无预警；其他视为异常
            if (!"200".equals(codeStr)) {
                return Collections.emptyList();
            }
            if (!respJson.has("warning") || !respJson.get("warning").isJsonArray()) {
                return Collections.emptyList();
            }
            JsonArray arr = respJson.getAsJsonArray("warning");
            List<QWeatherWarning> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject w = arr.get(i).getAsJsonObject();
                QWeatherWarning warning = new QWeatherWarning();
                warning.setWarnId(getStr(w, "id"));
                // typeName 更直观（暴雨/高温），type 是英文代码
                String typeName = getStr(w, "typeName");
                warning.setWarnType(typeName != null && !typeName.isEmpty() ? typeName : getStr(w, "type"));
                warning.setWarnLevel(getStr(w, "level"));
                warning.setTitle(getStr(w, "title"));
                warning.setAlertStart(parseTime(getStr(w, "startTime")));
                warning.setAlertEnd(parseTime(getStr(w, "endTime")));
                warning.setDescText(getStr(w, "text"));
                list.add(warning);
            }
            return list;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("和风天气查询异常 location={}", locationCodeOrName, e);
            return Collections.emptyList();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public List<CityLookupVO> lookupCity(String name) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("your-qweather")) {
            log.warn("未配置和风天气 api-key，跳过城市查询");
            return Collections.emptyList();
        }
        if (name == null || name.trim().isEmpty()) {
            return Collections.emptyList();
        }
        HttpURLConnection conn = null;
        try {
            String urlStr = GEO_BASE_URL + "/v2/city/lookup?location="
                    + URLEncoder.encode(name, "UTF-8")
                    + "&key=" + URLEncoder.encode(apiKey, "UTF-8");
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String resp = readAll(is);
            if (code < 200 || code >= 300) {
                log.error("和风城市查询失败 code={} resp={}", code, resp);
                return Collections.emptyList();
            }
            JsonObject respJson = JsonParser.parseString(resp).getAsJsonObject();
            if (!"200".equals(getStr(respJson, "code"))) {
                return Collections.emptyList();
            }
            if (!respJson.has("location") || !respJson.get("location").isJsonArray()) {
                return Collections.emptyList();
            }
            JsonArray arr = respJson.getAsJsonArray("location");
            List<CityLookupVO> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject c = arr.get(i).getAsJsonObject();
                CityLookupVO vo = new CityLookupVO();
                vo.setId(getStr(c, "id"));
                vo.setName(getStr(c, "name"));
                vo.setAdm(getStr(c, "adm"));
                vo.setCountry(getStr(c, "country"));
                list.add(vo);
            }
            return list;
        } catch (Exception e) {
            log.error("和风城市查询异常 name={}", name, e);
            return Collections.emptyList();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public String resolveLocationId(String nameOrCode) {
        if (nameOrCode == null || nameOrCode.trim().isEmpty()) {
            return null;
        }
        String v = nameOrCode.trim();
        // 纯数字视为和风城市ID，直接返回
        if (v.matches("\\d+")) {
            return v;
        }
        String cached = cityCodeCache.get(v);
        if (cached != null) {
            return cached;
        }
        List<CityLookupVO> list = lookupCity(v);
        if (list == null || list.isEmpty()) {
            return null;
        }
        String id = list.get(0).getId();
        if (id != null && !id.isEmpty()) {
            cityCodeCache.put(v, id);
        }
        return id;
    }

    private String getStr(JsonObject o, String key) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            return o.get(key).getAsString();
        }
        return null;
    }

    private Date parseTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat(TIME_PATTERN).parse(time);
        } catch (Exception e) {
            // 兼容不带时区的格式 yyyy-MM-ddTHH:mm
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(time);
            } catch (Exception ex) {
                return null;
            }
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
}
