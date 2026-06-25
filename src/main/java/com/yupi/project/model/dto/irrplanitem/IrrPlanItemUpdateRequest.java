package com.yupi.project.model.dto.irrplanitem;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 浇水时段更新请求
 *
 * @TableName irr_plan_item
 */
@Data
public class IrrPlanItemUpdateRequest implements Serializable {
    /**
     * 浇水时段自增ID
     */
    private Long id;

    /**
     * 关联对应方案的主键ID
     */
    private Long parentId;

    /**
     * 方案类型 1人工/2AI常态/3极端临时
     */
    private Integer parentType;

    /**
     * 每日浇水时刻 HH:mm:00
     */
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    private Date waterTime;

    /**
     * 单次浇水分钟，代表浇水量
     */
    private Integer waterDuration;

    /**
     * 前端展示排序
     */
    private Integer sort;

    /**
     * 该时段是否启用
     */
    private Integer enable;

    private static final long serialVersionUID = 1L;
}
