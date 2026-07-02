import request from "./request";
import type { WarnHistory, WarnSimulateRequest, PageResponse } from "@/types";

// 模拟预警
export function simulateWarn(data: WarnSimulateRequest) {
  return request.post<unknown, boolean>("/warn/simulate", data);
}

// 模拟天气恢复
export function simulateClear() {
  return request.post<unknown, boolean>("/warn/simulateClear");
}

// 手动解除当前预警（作废临时方案+恢复原方案）
export function manualCancel() {
  return request.post<unknown, boolean>("/warn/manualCancel");
}

// 预警历史分页
export function listWarnHistoryPage(current: number, size: number) {
  return request.get<unknown, PageResponse<WarnHistory>>("/warnHistory/list/page", { params: { current, size } });
}

// 预警历史列表
export function listWarnHistory() {
  return request.get<unknown, WarnHistory[]>("/warnHistory/list");
}

// 手动作废一条预警（标记 is_valid=0）
export function dismissWarn(id: number) {
  return request.post<unknown, boolean>("/warnHistory/dismiss", { id });
}
