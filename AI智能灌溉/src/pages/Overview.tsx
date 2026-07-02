import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import {
  LayoutDashboard,
  Send,
  Sparkles,
  Hand,
  Clock4,
  AlertTriangle,
  Droplets,
  ChevronRight,
  MapPin,
  Leaf,
} from "lucide-react";
import PageHeader from "@/components/PageHeader";
import DeviceStatusBadge from "@/components/DeviceStatusBadge";
import Skeleton from "@/components/Skeleton";
import { useStore } from "@/store";
import { usePolling } from "@/hooks/usePolling";
import { getManualPlan, getAiPlan, publishPlan, listPlanItems, listTempPlans } from "@/api/plan";
import { listWarnHistory } from "@/api/warn";
import { PLAN_TYPE, PLAN_TYPE_NAME, PLANT_TYPE_NAME, SCENE_NAME, type PlanItem, type WarnHistory } from "@/types";
import { sortItemsByTime, timeAgo, warnLevelBg } from "@/utils/format";
import { cn } from "@/lib/utils";

export default function Overview() {
  const navigate = useNavigate();
  const { config, loadConfig, pushToast } = useStore();
  const [items, setItems] = useState<PlanItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState(false);
  const [recentWarns, setRecentWarns] = useState<WarnHistory[]>([]);

  const planType = config?.currentPlanType ?? PLAN_TYPE.MANUAL;

  const loadCurrentItems = async () => {
    try {
      let result: PlanItem[] = [];
      if (planType === PLAN_TYPE.MANUAL) {
        const plan = await getManualPlan();
        result = await listPlanItems(plan?.id ?? 1, 1);
      } else if (planType === PLAN_TYPE.AI) {
        const plan = await getAiPlan();
        result = await listPlanItems(plan?.id ?? 1, 2);
      } else if (planType === PLAN_TYPE.TEMP) {
        // 临时方案：取生效中（status=1）的最新一条
        const list = await listTempPlans();
        const active = (list ?? []).find((p) => p.status === 1);
        if (active?.id) {
          result = await listPlanItems(active.id, 3);
        }
      }
      setItems(result);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  const loadRecentWarns = async () => {
    try {
      const list = await listWarnHistory();
      setRecentWarns((list ?? []).slice(0, 4));
    } catch {
      // 静默
    }
  };

  useEffect(() => {
    loadCurrentItems();
    loadRecentWarns();
  }, [planType]);

  usePolling(loadConfig, 60000);

  const handlePublish = async () => {
    setPublishing(true);
    try {
      await publishPlan();
      pushToast("success", "方案已下发至设备");
    } catch (e) {
      pushToast("error", (e as Error).message || "下发失败");
    } finally {
      setPublishing(false);
    }
  };

  const sortedItems = sortItemsByTime(items);
  const totalDuration = items.reduce((s, i) => s + (i.waterDuration ?? 0), 0);
  const planIcon = planType === PLAN_TYPE.AI ? Sparkles : planType === PLAN_TYPE.TEMP ? Clock4 : Hand;
  const PlanIcon = planIcon;

  return (
    <div>
      <PageHeader
        title="总览"
        subtitle="当前执行方案 · 设备状态 · 当前浇水计划"
        icon={<LayoutDashboard size={22} />}
        actions={
          <button
            onClick={handlePublish}
            disabled={publishing}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-blue-500 to-blue-400 text-white font-body font-medium text-sm shadow-blue-500/25 hover:opacity-90 transition-opacity disabled:opacity-50"
          >
            <Send size={16} />
            {publishing ? "下发中…" : "一键下发"}
          </button>
        }
      />

      {/* 当前方案卡片 */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="glass rounded-3xl p-7 mb-6 relative overflow-hidden"
      >
        <div className="absolute top-0 right-0 w-64 h-64 bg-blue-500/5 rounded-full blur-3xl -mr-20 -mt-20" />
        <div className="relative flex flex-wrap items-start justify-between gap-6">
          <div className="flex items-start gap-5">
            <div className={cn(
              "flex h-16 w-16 items-center justify-center rounded-2xl",
              planType === PLAN_TYPE.AI ? "bg-blue-400/15 text-blue-400" : planType === PLAN_TYPE.TEMP ? "bg-amber/15 text-amber" : "bg-blue-500/15 text-blue-400"
            )}>
              <PlanIcon size={28} />
            </div>
            <div>
              <span className="text-creamDim text-xs font-body uppercase tracking-wider">当前执行方案</span>
              <h2 className="font-display text-3xl text-cream font-medium mt-1">
                {PLAN_TYPE_NAME[planType] ?? "未配置"}
              </h2>
              <div className="flex items-center gap-4 mt-2 text-xs text-creamDim font-body">
                {config?.plantName && (
                  <span className="flex items-center gap-1">
                    <Leaf size={12} /> {config.plantName}
                  </span>
                )}
                {config?.locationName && (
                  <span className="flex items-center gap-1">
                    <MapPin size={12} /> {config.locationName}
                  </span>
                )}
                {config?.sceneType && (
                  <span className="px-2 py-0.5 rounded-full bg-cream/5 border border-cream/10">
                    {SCENE_NAME[config.sceneType]}
                  </span>
                )}
                {config?.plantType && (
                  <span className="px-2 py-0.5 rounded-full bg-cream/5 border border-cream/10">
                    {PLANT_TYPE_NAME[config.plantType]}
                  </span>
                )}
              </div>
            </div>
          </div>
          <div className="flex gap-8">
            <div className="text-center">
              <div className="font-display text-3xl text-blue-400 font-semibold">{loading ? "—" : sortedItems.length}</div>
              <div className="text-creamDim text-xs mt-1">浇水时段</div>
            </div>
            <div className="text-center">
              <div className="font-display text-3xl text-blue-400 font-semibold">{loading ? "—" : `${totalDuration}ml`}</div>
              <div className="text-creamDim text-xs mt-1">总浇水量</div>
            </div>
          </div>
        </div>
      </motion.div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 今日时间轴 */}
        <div className="lg:col-span-2 glass rounded-3xl p-6">
          <div className="flex items-center justify-between mb-5">
            <h3 className="font-display text-xl text-cream">当前浇水计划</h3>
            <Link to={planType === PLAN_TYPE.AI ? "/ai" : planType === PLAN_TYPE.TEMP ? "/temp" : "/manual"} className="text-blue-400 text-xs font-body flex items-center gap-1 hover:gap-2 transition-all">
              查看详情 <ChevronRight size={12} />
            </Link>
          </div>
          {loading ? (
            <div className="space-y-3">
              <Skeleton lines={3} />
            </div>
          ) : sortedItems.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Droplets size={32} className="text-ash mb-3" />
              <p className="text-creamDim text-sm">暂无浇水时段</p>
              <button onClick={() => navigate(planType === PLAN_TYPE.AI ? "/ai" : planType === PLAN_TYPE.TEMP ? "/temp" : "/manual")} className="mt-3 text-blue-400 text-xs hover:underline">
                去配置方案 →
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              {sortedItems.map((item, idx) => {
                const timeStr = item.waterTime?.substring(0, 5) ?? "--:--";
                const duration = item.waterDuration ?? 0;
                return (
                  <div key={idx} className="flex items-center gap-4 group">
                    <div className="font-mono text-sm text-blue-400 w-12 shrink-0">{timeStr}</div>
                    <div className="flex-1 h-2 rounded-full bg-cream/5 relative overflow-hidden">
                      <div className="absolute inset-y-0 left-0 bg-gradient-to-r from-blue-400/60 to-blue-400 rounded-full" style={{ width: `${Math.min(100, (duration / 100) * 100)}%` }} />
                    </div>
                    <div className="text-cream text-sm font-body w-16 text-right">{duration}ml</div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* 最新预警 + 设备 */}
        <div className="space-y-6">
          <DeviceStatusBadge />
          <div className="glass rounded-2xl p-5">
            <div className="flex items-center justify-between mb-4">
              <span className="text-creamDim text-sm font-body">最新预警</span>
              <Link to="/warn" className="text-blue-400 text-xs hover:underline">全部</Link>
            </div>
            {recentWarns.length === 0 ? (
              <div className="flex items-center gap-2 py-4 text-creamDim text-sm">
                <AlertTriangle size={16} className="text-ash" /> 暂无预警记录
              </div>
            ) : (
              <div className="space-y-2.5">
                {recentWarns.map((w, i) => (
                  <div key={i} className="flex items-center gap-2.5">
                    <span className={cn("text-[10px] px-2 py-0.5 rounded-full border font-medium", warnLevelBg(w.warnLevel))}>
                      {w.warnLevel || "未知"}
                    </span>
                    <span className="text-cream text-xs flex-1 truncate">{w.warnType}</span>
                    <span className="text-creamDim text-[10px] font-mono shrink-0">{timeAgo(w.recordTime)}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
