package com.yupi.project.model.dto.irrplanitem;

import com.yupi.project.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 浇水时段查询请求
 *
 * @TableName irr_plan_item
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class IrrPlanItemQueryRequest extends PageRequest implements Serializable {
    /**
     * 关联对应方案的主键ID
     */
    private Long parentId;

    /**
     * 方案类型 1人工/2AI常态/3极端临时
     */
    private Integer parentType;

    /**
     * 该时段是否启用
     */
    private Integer enable;

    private static final long serialVersionUID = 1L;
}
