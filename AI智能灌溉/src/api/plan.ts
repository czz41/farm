import request from "./request";
import type { IrrManualPlan, IrrAiPlan, IrrTempPlan, PlanItem, PageResponse } from "@/types";

// ===== 人工方案 =====
export function getManualPlan() {
  return request.get<unknown, IrrManualPlan>("/irrManualPlan/get");
}
export function addManualPlan(data: Partial<IrrManualPlan>) {
  return request.post<unknown, boolean>("/irrManualPlan/add", data);
}
export function updateManualPlan(data: Partial<IrrManualPlan>) {
  return request.post<unknown, boolean>("/irrManualPlan/update", data);
}

// ===== AI 方案 =====
export function getAiPlan() {
  return request.get<unknown, IrrAiPlan>("/irrAiPlan/get");
}
export function addAiPlan(data: Partial<IrrAiPlan>) {
  return request.post<unknown, boolean>("/irrAiPlan/add", data);
}
export function updateAiPlan(data: Partial<IrrAiPlan>) {
  return request.post<unknown, boolean>("/irrAiPlan/update", data);
}
export function generateAiPlan() {
  return request.post<unknown, IrrAiPlan>("/irrAiPlan/generate");
}

// ===== 临时方案 =====
export function getTempPlan(id: number) {
  return request.get<unknown, IrrTempPlan>("/irrTempPlan/get", { params: { id } });
}
export function listTempPlans() {
  return request.get<unknown, IrrTempPlan[]>("/irrTempPlan/list");
}
export function listTempPlansPage(current: number, size: number) {
  return request.get<unknown, PageResponse<IrrTempPlan>>("/irrTempPlan/list/page", { params: { current, size } });
}
export function deleteTempPlan(id: number) {
  return request.post<unknown, boolean>("/irrTempPlan/delete", { id });
}
export function updateTempPlan(data: Partial<IrrTempPlan>) {
  return request.post<unknown, boolean>("/irrTempPlan/update", data);
}

// ===== 时段明细 =====
export function addPlanItem(data: Partial<PlanItem>) {
  return request.post<unknown, boolean>("/irrPlanItem/add", data);
}
export function deletePlanItem(id: number) {
  return request.post<unknown, boolean>("/irrPlanItem/delete", { id });
}
export function updatePlanItem(data: Partial<PlanItem>) {
  return request.post<unknown, boolean>("/irrPlanItem/update", data);
}
export function listPlanItems(parentId: number, parentType: number) {
  return request.get<unknown, PlanItem[]>("/irrPlanItem/list", { params: { parentId, parentType } });
}

// ===== 下发 =====
export function publishPlan() {
  return request.post<unknown, boolean>("/irrPlan/publish");
}
export function activateTempPlan(tempPlanId: number) {
  return request.post<unknown, boolean>("/irrPlan/activateTemp", null, { params: { tempPlanId } });
}
