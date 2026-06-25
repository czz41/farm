// 系统配置
export interface SysConfig {
  id: number;
  plantName?: string;
  locationCode?: string;
  locationName?: string;
  specialNote?: string;
  sceneType?: number; // 1=室外 2=室内
  enableWarn?: number; // 0=关 1=开
  enableAutoIntervene?: number; // 0=关 1=开
  currentPlanType?: number; // 1=人工 2=AI 3=临时
  mailAddr?: string;
  createTime?: string;
  updateTime?: string;
}

// 城市查询
export interface CityLookupVO {
  id: string;
  name: string;
  adm: string;
  country: string;
}

// 方案时段明细（后端字段名）
export interface PlanItem {
  id?: number;
  parentId?: number;
  parentType?: number; // 1人工 2AI 3临时
  waterTime?: string; // HH:mm:ss
  waterDuration?: number; // 浇水分钟数
  sort?: number;
  enable?: number;
}

// 人工方案
export interface IrrManualPlan {
  id?: number;
  name?: string;
  items?: PlanItem[];
  createTime?: string;
  updateTime?: string;
}

// AI方案
export interface IrrAiPlan {
  id?: number;
  name?: string;
  prompt?: string;
  items?: PlanItem[];
  createTime?: string;
  updateTime?: string;
}

// 临时方案
export interface IrrTempPlan {
  id?: number;
  sourceType?: number;
  warnType?: string;
  warnLevel?: string;
  alertStart?: string;
  alertEnd?: string;
  status?: number; // 1=生效 2=失效
  descText?: string;
  createTime?: string;
  items?: PlanItem[];
}

// 预警历史
export interface WarnHistory {
  id?: number;
  warnId?: string;
  warnType?: string;
  warnLevel?: string;
  msgType?: string; // alert / cancel
  alertStart?: string;
  alertEnd?: string;
  descText?: string;
  recordTime?: string;
}

// 分页响应
export interface PageResponse<T> {
  records: T[];
  total: number;
  current?: number;
  size?: number;
}

// 通用响应
export interface BaseResponse<T> {
  code: number;
  data: T;
  message?: string;
}

// 设备状态
export interface DeviceStatusVO {
  online: boolean;
  lastSeen: number;
  lastPayload: string;
}

// 模拟预警请求
export interface WarnSimulateRequest {
  warnType: string;
  warnLevel: string;
  durationMinutes: number;
  descText: string;
}

// 方案类型常量
export const PLAN_TYPE = {
  MANUAL: 1,
  AI: 2,
  TEMP: 3,
} as const;

export const SCENE = {
  OUTDOOR: 1,
  INDOOR: 2,
} as const;

// 方案类型名称
export const PLAN_TYPE_NAME: Record<number, string> = {
  1: "人工方案",
  2: "AI方案",
  3: "临时方案",
};

// 场景名称
export const SCENE_NAME: Record<number, string> = {
  1: "室外",
  2: "室内",
};
