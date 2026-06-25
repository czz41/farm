import request from "./request";

export interface OperationLog {
  id?: number;
  operationType?: string;
  content?: string;
  createTime?: string;
}

// 获取操作日志列表
export function listOperationLogs() {
  return request.get<unknown, OperationLog[]>("/sysOperationLog/list");
}
