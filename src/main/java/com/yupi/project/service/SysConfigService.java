package com.yupi.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.project.model.entity.SysConfig;

/**
 * @description 针对表【sys_config(系统全局配置)】的数据库操作Service
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 校验
     *
     * @param sysConfig
     * @param add 是否为创建校验
     */
    void validSysConfig(SysConfig sysConfig, boolean add);
}
