package com.yupi.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.project.common.ErrorCode;
import com.yupi.project.exception.BusinessException;
import com.yupi.project.mapper.SysConfigMapper;
import com.yupi.project.model.entity.SysConfig;
import com.yupi.project.service.SysConfigService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @description 针对表【sys_config(系统全局配置)】的数据库操作Service实现
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public void validSysConfig(SysConfig sysConfig, boolean add) {
        if (sysConfig == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String plantName = sysConfig.getPlantName();
        String locationCode = sysConfig.getLocationCode();
        Integer sceneType = sysConfig.getSceneType();
        Integer currentPlanType = sysConfig.getCurrentPlanType();
        if (add) {
            if (StringUtils.isAnyBlank(plantName, locationCode) || ObjectUtils.anyNull(sceneType, currentPlanType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (sceneType != null && sceneType != 1 && sceneType != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "场景类型不合法");
        }
        if (currentPlanType != null && (currentPlanType < 1 || currentPlanType > 3)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前方案类型不合法");
        }
    }
}
