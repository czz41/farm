// 格式化时间戳为可读字符串
export function formatTimestamp(ts?: number | string | null): string {
  if (!ts) return "暂无";
  const d = new Date(typeof ts === "string" ? ts.replace(/-/g, "/") : ts);
  if (isNaN(d.getTime())) return "暂无";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

// 格式化为 HH:mm
export function toHHmm(date: Date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// 相对时间（如"3分钟前"）
export function timeAgo(ts?: number | string | null): string {
  if (!ts) return "暂无";
  const d = new Date(typeof ts === "string" ? ts.replace(/-/g, "/") : ts);
  if (isNaN(d.getTime())) return "暂无";
  const diff = Date.now() - d.getTime();
  if (diff < 0) return formatTimestamp(ts);
  const min = Math.floor(diff / 60000);
  if (min < 1) return "刚刚";
  if (min < 60) return `${min}分钟前`;
  const hour = Math.floor(min / 60);
  if (hour < 24) return `${hour}小时前`;
  const day = Math.floor(hour / 24);
  return `${day}天前`;
}

// 水量格式化
export function formatWater(ml: number): string {
  if (ml >= 1000) return `${(ml / 1000).toFixed(1)}L`;
  return `${ml}ml`;
}

// 预警等级颜色映射
export function warnLevelColor(level?: string): string {
  if (!level) return "text-creamDim";
  if (level.includes("红") || level.toLowerCase().includes("red")) return "text-rust";
  if (level.includes("橙") || level.toLowerCase().includes("orange")) return "text-amber";
  if (level.includes("黄") || level.toLowerCase().includes("yellow")) return "text-amber";
  if (level.includes("蓝") || level.toLowerCase().includes("blue")) return "text-blue-400";
  return "text-creamDim";
}

// 预警等级背景色
export function warnLevelBg(level?: string): string {
  if (!level) return "bg-ash/20 text-creamDim border-ash/30";
  if (level.includes("红") || level.toLowerCase().includes("red")) return "bg-rust/15 text-rust border-rust/30";
  if (level.includes("橙") || level.toLowerCase().includes("orange")) return "bg-amber/15 text-amber border-amber/30";
  if (level.includes("黄") || level.toLowerCase().includes("yellow")) return "bg-amber/15 text-amber border-amber/30";
  if (level.includes("蓝") || level.toLowerCase().includes("blue")) return "bg-blue-500/15 text-blue-400 border-blue-500/30";
  return "bg-ash/20 text-creamDim border-ash/30";
}

// 时段排序：按 waterTime 升序
export function sortItemsByTime<T extends { waterTime?: string }>(items: T[]): T[] {
  return [...items].sort((a, b) => (a.waterTime || "").localeCompare(b.waterTime || ""));
}

// 当前时间是否在某个时段范围内（前后 30 分钟视为"进行中"）
export function isSlotActive(startTime: string, now: Date = new Date()): boolean {
  const [h, m] = startTime.split(":").map(Number);
  const slot = new Date(now);
  slot.setHours(h, m, 0, 0);
  const diff = Math.abs(now.getTime() - slot.getTime());
  return diff <= 30 * 60 * 1000;
}

// 时段是否已过
export function isSlotPassed(startTime: string, now: Date = new Date()): boolean {
  const [h, m] = startTime.split(":").map(Number);
  const slot = new Date(now);
  slot.setHours(h, m, 0, 0);
  return slot.getTime() < now.getTime();
}
