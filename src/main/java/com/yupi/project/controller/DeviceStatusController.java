package com.yupi.project.controller;

import com.yupi.project.common.BaseResponse;
import com.yupi.project.common.DeviceStatusHolder;
import com.yupi.project.common.ResultUtils;
import com.yupi.project.model.vo.DeviceStatusVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 设备状态接口
 */
@RestController
@RequestMapping("/device")
public class DeviceStatusController {

    @Value("${mqtt.online-timeout:60000}")
    private long onlineTimeout;

    @Resource
    private DeviceStatusHolder deviceStatusHolder;

    /**
     * 查询设备在线状态
     *
     * @return
     */
    @GetMapping("/status")
    public BaseResponse<DeviceStatusVO> status() {
        DeviceStatusVO vo = new DeviceStatusVO();
        long last = deviceStatusHolder.getLastSeenTs();
        vo.setLastSeen(last);
        vo.setLastPayload(deviceStatusHolder.getLastPayload());
        vo.setOnline(last > 0 && (System.currentTimeMillis() - last) <= onlineTimeout);
        return ResultUtils.success(vo);
    }
}
