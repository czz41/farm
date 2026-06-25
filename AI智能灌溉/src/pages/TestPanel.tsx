import { useState, useEffect, useRef } from "react";
import { motion } from "framer-motion";
import {
  FlaskConical,
  Send,
  CloudSun,
  Terminal,
  Radio,
  Loader2,
  AlertTriangle,
} from "lucide-react";
import PageHeader from "@/components/PageHeader";
import DeviceStatusBadge from "@/components/DeviceStatusBadge";
import { useStore } from "@/store";
import { simulateWarn } from "@/api/warn";
import { publishPlan } from "@/api/plan";
import { listOperationLogs, type OperationLog } from "@/api/operationLog";
import type { WarnSimulateRequest } from "@/types";
import { cn } from "@/lib/utils";

interface LogEntry {
  time: string;
  type: "info" | "success" | "error" | "warn";
  message: string;
}

const WARN_TYPES = ["暴雨", "高温", "大风", "雷电", "冰雹", "寒潮", "暴雪", "干旱"];
const WARN_LEVELS = ["蓝色", "黄色", "橙色", "红色"];

export default function TestPanel() {
  const { config, pushToast, loadConfig } = useStore();
  const [form, setForm] = useState<WarnSimulateRequest>({
    warnType: "暴雨",
    warnLevel: "橙色",
    durationMinutes: 60,
    descText: "",
  });
  // 前端以小时为单位编辑，提交时 ×60 转为分钟
  const [durationHours, setDurationHours] = useState(1);
  const [sending, setSending] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const logEndRef = useRef<HTMLDivElement>(null);

  const addLog = (type: LogEntry["type"], message: string) => {
    const now = new Date();
    const time = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}:${String(now.getSeconds()).padStart(2, "0")}`;
    setLogs((l) => [...l, { time, type, message }].slice(-50));
  };

  // 从后端加载操作日志
  const loadLogs = async () => {
    try {
      const data = await listOperationLogs();
      if (data && data.length > 0) {
        const entries: LogEntry[] = data.map((log: OperationLog) => {
          const t = log.createTime ? new Date(log.createTime) : new Date();
          const time = `${String(t.getHours()).padStart(2, "0")}:${String(t.getMinutes()).padStart(2, "0")}:${String(t.getSeconds()).padStart(2, "0")}`;
          const type: LogEntry["type"] = log.operationType === "simulate_warn" ? "warn"
            : log.operationType === "simulate_clear" ? "success"
            : log.operationType === "publish" ? "success"
            : "info";
          return { time, type, message: log.content ?? "" };
        }).reverse();
        setLogs(entries);
      }
    } catch {
      // 静默
    }
  };

  useEffect(() => {
    loadLogs();
  }, []);

  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [logs]);

  const handleSimulate = async () => {
    setSending(true);
    addLog("info", `发送模拟预警：${form.warnType} · ${form.warnLevel} · 持续${durationHours}小时`);
    try {
      await simulateWarn({ ...form, durationMinutes: durationHours * 60 });
      addLog("success", "模拟预警已发送，后端将记录预警并触发介入逻辑");
      if (config?.enableAutoIntervene) {
        addLog("warn", `自动介入已开启：${config.currentPlanType === 2 ? "AI模式将自动切换下发临时方案" : "人工模式生成临时方案待确认"}`);
      } else {
        addLog("info", "自动介入未开启，仅记录预警与邮件通知");
      }
      pushToast("success", "模拟预警已发送");
      await loadConfig();
      await loadLogs();
    } catch (e) {
      addLog("error", `发送失败：${(e as Error).message}`);
      pushToast("error", (e as Error).message || "发送失败");
    } finally {
      setSending(false);
    }
  };

  const handlePublish = async () => {
    setPublishing(true);
    addLog("info", "下发当前方案…");
    try {
      await publishPlan();
      addLog("success", "当前方案已下发至设备");
      pushToast("success", "方案已下发");
      await loadLogs();
    } catch (e) {
      addLog("error", `下发失败：${(e as Error).message}`);
      pushToast("error", (e as Error).message || "下发失败");
    } finally {
      setPublishing(false);
    }
  };

  const logColor = (type: LogEntry["type"]) =>
    type === "success" ? "text-blue-400" : type === "error" ? "text-rust" : type === "warn" ? "text-amber" : "text-blue-400";

  return (
    <div>
      <PageHeader
        title="测试面板"
        subtitle="模拟极端天气预警 · 设备状态监控 · 快捷下发"
        icon={<FlaskConical size={22} />}
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 模拟预警表单 */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="lg:col-span-2 glass rounded-3xl p-6">
          <div className="flex items-center gap-2 mb-5">
            <CloudSun size={18} className="text-amber" />
            <h3 className="font-display text-lg text-cream">模拟极端天气预警</h3>
          </div>

          <div className="space-y-5">
            {/* 预警类型 */}
            <div>
              <label className="block text-creamDim text-xs mb-2">预警类型</label>
              <div className="flex flex-wrap gap-2">
                {WARN_TYPES.map((t) => (
                  <button
                    key={t}
                    onClick={() => setForm((f) => ({ ...f, warnType: t }))}
                    className={cn(
                      "px-3 py-1.5 rounded-lg text-xs transition-all",
                      form.warnType === t ? "bg-amber/20 text-amber border border-amber/40" : "bg-cream/5 text-creamDim border border-cream/10 hover:border-cream/20"
                    )}
                  >
                    {t}
                  </button>
                ))}
              </div>
            </div>

            {/* 预警等级 */}
            <div>
              <label className="block text-creamDim text-xs mb-2">预警等级</label>
              <div className="flex gap-2">
                {WARN_LEVELS.map((l) => (
                  <button
                    key={l}
                    onClick={() => setForm((f) => ({ ...f, warnLevel: l }))}
                    className={cn(
                      "flex-1 py-2 rounded-lg text-xs transition-all border",
                      form.warnLevel === l
                        ? l === "红色" ? "bg-rust/20 text-rust border-rust/40"
                          : l === "橙色" ? "bg-amber/20 text-amber border-amber/40"
                          : l === "黄色" ? "bg-amber/20 text-amber border-amber/40"
                          : "bg-blue-500/20 text-blue-400 border-blue-500/40"
                        : "bg-cream/5 text-creamDim border-cream/10 hover:border-cream/20"
                    )}
                  >
                    {l}
                  </button>
                ))}
              </div>
            </div>

            {/* 持续时长 */}
            <div>
              <label className="block text-creamDim text-xs mb-2">持续时长（小时）</label>
              <div className="flex items-center gap-3">
                <input
                  type="range"
                  min={1}
                  max={72}
                  step={1}
                  value={durationHours}
                  onChange={(e) => setDurationHours(Number(e.target.value))}
                  className="flex-1 accent-blue-500"
                />
                <span className="font-mono text-cream text-sm w-16 text-right">{durationHours}h</span>
              </div>
            </div>

            {/* 描述 */}
            <div>
              <label className="block text-creamDim text-xs mb-2">预警描述（可选）</label>
              <textarea
                value={form.descText}
                onChange={(e) => setForm((f) => ({ ...f, descText: e.target.value }))}
                placeholder="如：预计未来2小时内降雨量达50毫米以上…"
                rows={2}
                className="w-full bg-cream/5 border border-cream/10 rounded-xl px-4 py-2.5 text-cream text-sm font-body outline-none focus:border-amber/40 resize-none"
              />
            </div>

            {/* 操作按钮 */}
            <div className="flex gap-3 pt-2">
              <button
                onClick={handleSimulate}
                disabled={sending}
                className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl bg-gradient-to-r from-amber to-rust text-cream font-body font-medium text-sm hover:opacity-90 transition-opacity disabled:opacity-50"
              >
                {sending ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
                {sending ? "发送中…" : "发送模拟预警"}
              </button>
            </div>
          </div>
        </motion.div>

        {/* 右侧：设备状态 + 快捷操作 */}
        <div className="space-y-6">
          <DeviceStatusBadge />
          <div className="glass rounded-2xl p-5">
            <div className="flex items-center gap-2 mb-4">
              <Radio size={16} className="text-blue-400" />
              <span className="text-creamDim text-sm font-body">快捷操作</span>
            </div>
            <button
              onClick={handlePublish}
              disabled={publishing}
              className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl bg-blue-500/15 text-blue-400 hover:bg-blue-500/25 transition-colors text-sm font-body font-medium disabled:opacity-50"
            >
              {publishing ? <Loader2 size={15} className="animate-spin" /> : <Send size={15} />}
              下发当前方案
            </button>
            <div className="mt-4 space-y-1.5 text-xs">
              <div className="flex justify-between">
                <span className="text-creamDim">当前模式</span>
                <span className="text-cream">{config?.currentPlanType === 1 ? "人工" : config?.currentPlanType === 2 ? "AI" : config?.currentPlanType === 3 ? "临时" : "—"}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-creamDim">自动介入</span>
                <span className={config?.enableAutoIntervene ? "text-blue-400" : "text-creamDim"}>{config?.enableAutoIntervene ? "已开启" : "未开启"}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 控制台日志 */}
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="glass rounded-3xl p-6 mt-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Terminal size={16} className="text-blue-400" />
            <h3 className="font-display text-lg text-cream">操作日志</h3>
          </div>
          {logs.length > 0 && (
            <button onClick={() => { setLogs([]); loadLogs(); }} className="text-creamDim text-xs hover:text-cream transition-colors">
              清空
            </button>
          )}
        </div>
        <div className="bg-bark/60 rounded-xl p-4 h-56 overflow-y-auto font-mono text-xs space-y-1.5">
          {logs.length === 0 ? (
            <div className="flex items-center gap-2 text-creamDim py-4">
              <AlertTriangle size={14} className="text-ash" /> 暂无操作记录，发送模拟预警后将在此显示
            </div>
          ) : (
            logs.map((log, i) => (
              <div key={i} className="flex gap-3">
                <span className="text-creamDim shrink-0">[{log.time}]</span>
                <span className={cn("shrink-0", logColor(log.type))}>
                  {log.type === "success" ? "✓" : log.type === "error" ? "✗" : log.type === "warn" ? "⚠" : "→"}
                </span>
                <span className="text-creamDim">{log.message}</span>
              </div>
            ))
          )}
          <div ref={logEndRef} />
        </div>
      </motion.div>
    </div>
  );
}
