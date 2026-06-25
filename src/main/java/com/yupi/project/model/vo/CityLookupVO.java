package com.yupi.project.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 和风天气城市查询结果
 */
@Data
public class CityLookupVO implements Serializable {

    /**
     * 和风城市ID
     */
    private String id;

    /**
     * 城市名称
     */
    private String name;

    /**
     * 上级行政区（省/州）
     */
    private String adm;

    /**
     * 国家
     */
    private String country;

    private static final long serialVersionUID = 1L;
}
