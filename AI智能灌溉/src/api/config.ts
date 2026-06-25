import request from "./request";
import type { SysConfig, CityLookupVO } from "@/types";

// 获取配置
export function getConfig() {
  return request.get<unknown, SysConfig>("/sysConfig/get");
}

// 新增配置
export function addConfig(data: Partial<SysConfig>) {
  return request.post<unknown, boolean>("/sysConfig/add", data);
}

// 更新配置
export function updateConfig(data: Partial<SysConfig>) {
  return request.post<unknown, boolean>("/sysConfig/update", data);
}

// 城市联想查询
export function cityLookup(name: string) {
  return request.get<unknown, CityLookupVO[]>("/sysConfig/cityLookup", { params: { name } });
}
