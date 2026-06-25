package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.mapper.WarnHistoryMapper;
import com.yupi.project.model.entity.WarnHistory;
import com.yupi.project.service.WarnHistoryService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @description 针对表【warn_history(历史预警记录表)】的数据库操作Service实现
 */
@Service
public class WarnHistoryServiceImpl extends ServiceImpl<WarnHistoryMapper, WarnHistory> implements WarnHistoryService {

    @Override
    public void validWarnHistory(WarnHistory warnHistory, boolean add) {
        if (warnHistory == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String warnId = warnHistory.getWarnId();
        String warnType = warnHistory.getWarnType();
        String warnLevel = warnHistory.getWarnLevel();
        Date alertStart = warnHistory.getAlertStart();
        Date alertEnd = warnHistory.getAlertEnd();
        String msgType = warnHistory.getMsgType();
        if (add) {
            if (StringUtils.isAnyBlank(warnId, warnType, warnLevel, msgType)
                    || ObjectUtils.anyNull(alertStart, alertEnd)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (alertStart != null && alertEnd != null && alertEnd.before(alertStart)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间不能早于开始时间");
        }
    }
}
