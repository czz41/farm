import request from "./request";
import type { DeviceStatusVO } from "@/types";

// 设备在线状态
export function getDeviceStatus() {
  return request.get<unknown, DeviceStatusVO>("/device/status");
}
