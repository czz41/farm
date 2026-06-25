import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Clock4, Zap, CheckCircle2, ChevronDown, ChevronUp, Send, AlertCircle, Droplets, ShieldOff } from "lucide-react";
import PageHeader from "@/components/PageHeader";
import Skeleton from "@/components/Skeleton";
import ConfirmDialog from "@/components/ConfirmDialog";
import { useStore } from "@/store";
import { listTempPlans, activateTempPlan, listPlanItems } from "@/api/plan";
import { manualCancel } from "@/api/warn";
import { PLAN_TYPE_NAME, type IrrTempPlan, type PlanItem } from "@/types";
import { formatTimestamp, sortItemsByTime, warnLevelBg } from "@/utils/format";
import { cn } from "@/lib/utils";

export default function TempPlan() {
  const { config, pushToast, loadConfig } = useStore();
  const [plans, setPlans] = useState<IrrTempPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState<number | null>(null);
  const [itemMap, setItemMap] = useState<Record<number, PlanItem[]>>({});
  const [activateId, setActivateId] = useState<number | null>(null);
  const [activating, setActivating] = useState(false);
  const [deactivateId, setDeactivateId] = useState<number | null>(null);
  const [deactivating, setDeactivating] = useState(false);

  const load = async () => {
    try {
      const list = await listTempPlans();
      setPlans(list ?? []);
    } catch {
      // 静默
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const toggleExpand = async (id: number) => {
    if (expanded === id) {
      setExpanded(null);
      return;
    }
    setExpanded(id);
    if (!itemMap[id]) {
      try {
        // 用 listPlanItems 获取临时方案时段明细（parentType=3）
        const items = await listPlanItems(id, 3);
        setItemMap((m) => ({ ...m, [id]: items ?? [] }));
      } catch {
        setItemMap((m) => ({ ...m, [id]: [] }));
      }
    }
  };

  const handleActivate = async () => {
    if (activateId === null) return;
    setActivating(true);
    try {
      await activateTempPlan(activateId);
      pushToast("success", "临时方案已切换并下发");
      await loadConfig();
      await load();
    } catch (e) {
      pushToast("error", (e as Error).message || "切换失败");
    } finally {
      setActivating(false);
      setActivateId(null);
    }
  };

  const handleDeactivate = async () => {
    if (deactivateId === null) return;
    setDeactivating(true);
    try {
      await manualCancel();
      pushToast("success", "临时方案已手动解除，已恢复原方案");
      await loadConfig();
      await load();
    } catch (e) {
      pushToast("error", (e as Error).message || "解除失败");
    } finally {
      setDeactivating(false);
      setDeactivateId(null);
    }
  };

  const isManualMode = config?.currentPlanType === 1;

  return (
    <div>
      <PageHeader
        title="临时方案"
        subtitle="极端天气触发的临时浇水方案 · 人工模式可一键切换下发"
        icon={<Clock4 size={22} />}
      />

      {/* 说明 */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="glass rounded-2xl p-4 mb-6 flex items-start gap-3"
      >
        <AlertCircle size={16} className="text-amber mt-0.5 shrink-0" />
        <p className="text-creamDim text-xs font-body leading-relaxed">
          {isManualMode
            ? "当前为人工模式。AI 生成的临时方案需手动确认后才会切换下发；气象恢复后发送邮件提示切回，无自动替换。"
            : "当前为 AI 模式。极端天气时 AI 会自动切换并下发临时方案，气象恢复后自动切回基准方案。"}
        </p>
      </motion.div>

      {loading ? (
        <div className="glass rounded-3xl p-6"><Skeleton lines={5} /></div>
      ) : plans.length === 0 ? (
        <div className="glass rounded-3xl py-16 flex flex-col items-center text-center">
          <Clock4 size={40} className="text-ash mb-4" />
          <p className="text-cream text-sm mb-1">暂无临时方案</p>
          <p className="text-creamDim text-xs">极端天气触发自动介入后，将在此生成临时方案</p>
        </div>
      ) : (
        <div className="space-y-4">
          {plans.map((plan, idx) => {
            const active = plan.status === 1;
            const isOpen = expanded === plan.id;
            const planItems = itemMap[plan.id!] ?? [];
            const sortedItems = sortItemsByTime(planItems);
            const canActivate = active && isManualMode && config?.currentPlanType !== 3;

            return (
              <motion.div
                key={plan.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: idx * 0.05 }}
                className={cn("glass rounded-2xl overflow-hidden", active && "border-amber/30")}
              >
                <div className="p-5">
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-2 flex-wrap">
                        <span className={cn("text-[10px] px-2 py-0.5 rounded-full border font-medium", warnLevelBg(plan.warnLevel))}>
                          {plan.warnLevel || "未知"}
                        </span>
                        <span className={cn(
                          "text-[10px] px-2 py-0.5 rounded-full font-medium",
                          active ? "bg-blue-500/15 text-blue-400" : "bg-ash/20 text-creamDim"
                        )}>
                          {active ? "生效中" : "已失效"}
                        </span>
                        <span className="text-[10px] text-creamDim px-2 py-0.5 rounded-full bg-cream/5">
                          来源：{PLAN_TYPE_NAME[plan.sourceType ?? 1]}
                        </span>
                      </div>
                      <h3 className="font-display text-lg text-cream mb-1">{plan.warnType || "极端天气临时方案"}</h3>
                      <div className="text-creamDim text-xs font-body space-y-0.5">
                        <div>预警区间：{formatTimestamp(plan.alertStart)} → {formatTimestamp(plan.alertEnd)}</div>
                        {plan.descText && <div className="truncate">描述：{plan.descText}</div>}
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-2 shrink-0">
                      {canActivate && (
                        <button
                          onClick={() => setActivateId(plan.id!)}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-amber/20 text-amber hover:bg-amber/30 transition-colors text-xs font-body font-medium"
                        >
                          <Zap size={13} /> 切换下发
                        </button>
                      )}
                      {active && (
                        <button
                          onClick={() => setDeactivateId(plan.id!)}
                          disabled={deactivating}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-rust/15 text-rust hover:bg-rust/25 transition-colors text-xs font-body font-medium disabled:opacity-50"
                        >
                          <ShieldOff size={13} /> 手动解除
                        </button>
                      )}
                      {active && config?.currentPlanType === 3 && (
                        <span className="flex items-center gap-1 text-blue-400 text-xs">
                          <CheckCircle2 size={13} /> 执行中
                        </span>
                      )}
                      <button
                        onClick={() => toggleExpand(plan.id!)}
                        className="p-1.5 rounded-lg text-creamDim hover:text-cream hover:bg-cream/5 transition-colors"
                      >
                        {isOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                      </button>
                    </div>
                  </div>
                </div>

                {/* 展开明细 */}
                {isOpen && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: "auto", opacity: 1 }}
                    className="border-t border-cream/5 px-5 py-4 bg-bark/20"
                  >
                    {sortedItems.length === 0 ? (
                      <div className="flex items-center gap-2 text-creamDim text-xs py-2">
                        <Droplets size={14} className="text-ash" /> 暂无时段明细
                      </div>
                    ) : (
                      <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                        {sortedItems.map((item, i) => {
                          const timeStr = item.waterTime?.substring(0, 5) ?? "--:--";
                          const duration = item.waterDuration ?? 0;
                          return (
                            <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-cream/5">
                              <div className="font-mono text-sm text-blue-400">{timeStr}</div>
                              <div className="text-cream text-sm">{duration}ml</div>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </motion.div>
                )}
              </motion.div>
            );
          })}
        </div>
      )}

      <ConfirmDialog
        open={activateId !== null}
        title="切换临时方案"
        message="确认切换并下发该临时方案吗？切换后将在极端天气期间执行此方案。"
        confirmText={activating ? "切换中…" : "确认切换"}
        onConfirm={handleActivate}
        onCancel={() => setActivateId(null)}
      />

      <ConfirmDialog
        open={deactivateId !== null}
        title="手动解除临时方案"
        message="确认手动解除该临时方案吗？将作废生效中的方案并恢复原浇水方案，同时发送邮件通知。"
        confirmText={deactivating ? "解除中…" : "确认解除"}
        onConfirm={handleDeactivate}
        onCancel={() => setDeactivateId(null)}
      />
    </div>
  );
}
