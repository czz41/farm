package com.yupi.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.project.model.entity.WarnHistory;

/**
 * @description 针对表【warn_history(历史预警记录表)】的数据库操作Service
 */
public interface WarnHistoryService extends IService<WarnHistory> {

    /**
     * 校验
     *
     * @param warnHistory
     * @param add 是否为创建校验
     */
    void validWarnHistory(WarnHistory warnHistory, boolean add);
}
