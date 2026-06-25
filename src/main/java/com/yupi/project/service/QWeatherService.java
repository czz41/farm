package com.yupi.project.service;

import com.yupi.project.model.vo.CityLookupVO;
import com.yupi.project.model.vo.QWeatherWarning;

import java.util.List;

/**
 * 和风天气预警查询服务
 */
public interface QWeatherService {

    /**
     * 查询指定城市的当前生效预警（极端天气）。
     * 入参既可以是和风城市ID（纯数字），也可以是城市名（如"杭州"），内部自动解析。
     *
     * @param locationCodeOrName 和风城市ID 或 城市名
     * @return 预警列表，无预警返回空列表
     */
    List<QWeatherWarning> queryWarnings(String locationCodeOrName);

    /**
     * 城市名查询：返回匹配的城市候选列表（含城市ID）
     *
     * @param name 城市名（中文或拼音）
     * @return 候选城市列表
     */
    List<CityLookupVO> lookupCity(String name);

    /**
     * 将"城市名 或 城市ID"统一解析为和风城市ID：
     * 纯数字直接返回；否则调用城市查询取首个匹配。
     *
     * @param nameOrCode 城市名 或 城市ID
     * @return 和风城市ID，解析失败返回 null
     */
    String resolveLocationId(String nameOrCode);
}
