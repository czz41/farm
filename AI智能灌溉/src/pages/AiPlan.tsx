import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Sparkles, Droplets, Info, Loader2, RefreshCw, Leaf } from "lucide-react";
import PageHeader from "@/components/PageHeader";
import PlanItemCard from "@/components/PlanItemCard";
import Skeleton from "@/components/Skeleton";
import { useStore } from "@/store";
import { updateConfig } from "@/api/config";
import { getAiPlan, generateAiPlan, listPlanItems } from "@/api/plan";
import type { PlanItem, IrrAiPlan } from "@/types";
import { sortItemsByTime } from "@/utils/format";

export default function AiPlan() {
  const { config, pushToast, loadConfig } = useStore();
  const [plan, setPlan] = useState<IrrAiPlan | null>(null);
  const [items, setItems] = useState<PlanItem[]>([]);
  const [planId, setPlanId] = useState<number>(1);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);

  const load = async () => {
    try {
      const data = await getAiPlan();
      setPlan(data);
      const pid = data?.id ?? 1;
      setPlanId(pid);
      // 单独获取时段明细（parentType=2 AI方案）
      const planItems = await listPlanItems(pid, 2);
      setItems(planItems ?? []);
    } catch {
      // 静默
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleGenerate = async () => {
    if (!config?.plantName) {
      pushToast("error", "请先在基础配置中填写植物信息");
      return;
    }
    setGenerating(true);
    try {
      const data = await generateAiPlan();
      setPlan(data);
      const pid = data?.id ?? planId;
      setPlanId(pid);
      // 重新拉取时段
      const planItems = await listPlanItems(pid, 2);
      setItems(planItems ?? []);
      pushToast("success", "AI方案已生成并下发");
      // 生成后自动下发AI方案
      try {
        await updateConfig({ currentPlanType: 2 });
        await loadConfig();
      } catch {
        pushToast("info", "已生成，下发失败请稍后手动下发");
      }
    } catch (e) {
      pushToast("error", (e as Error).message || "生成失败");
    } finally {
      setGenerating(false);
    }
  };

  const sorted = sortItemsByTime(items);
  const totalDuration = items.reduce((s, i) => s + (i.waterDuration ?? 0), 0);

  return (
    <div>
      <PageHeader
        title="AI方案"
        subtitle="DeepSeek 生成适配基准方案 · 多时段智能浇水计划"
        icon={<Sparkles size={22} />}
        actions={
          <button
            onClick={handleGenerate}
            disabled={generating}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl border border-blue-400/30 text-blue-400 hover:bg-blue-400/10 transition-colors text-sm font-body disabled:opacity-50"
          >
            {generating ? <Loader2 size={16} className="animate-spin" /> : <RefreshCw size={16} />}
            {generating ? "生成中…" : (items.length ? "重新生成" : "生成方案")}
          </button>
        }
      />

      {/* 提示横幅 */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="glass rounded-2xl p-4 mb-6 flex items-start gap-3"
      >
        <Info size={16} className="text-blue-400 mt-0.5 shrink-0" />
        <p className="text-creamDim text-xs font-body leading-relaxed">
          AI模式下，系统根据植物、地域、养护信息调用 DeepSeek 生成适配基准方案。
          极端天气时（开启自动介入），AI 生成临时方案并<span className="text-cream">自动下发</span>，
          邮件告知已自动更新；气象恢复后<span className="text-cream">自动切回</span>基准方案并推送恢复邮件。
          室内场景下，AI 方案会自动下调浇水量适配室内环境。
        </p>
      </motion.div>

      {/* 生成中骨架屏 */}
      {generating ? (
        <div className="space-y-4">
          <div className="glass rounded-3xl p-6">
            <div className="flex items-center gap-3 mb-4">
              <Loader2 size={20} className="text-blue-400 animate-spin" />
              <span className="text-cream font-body">DeepSeek 正在生成浇水方案…</span>
            </div>
            <Skeleton lines={4} />
          </div>
        </div>
      ) : loading ? (
        <div className="glass rounded-3xl p-6"><Skeleton lines={4} /></div>
      ) : sorted.length === 0 ? (
        <div className="glass rounded-3xl py-16 flex flex-col items-center text-center">
          <div className="relative mb-4">
            <Sparkles size={40} className="text-blue-400/40" />
            <div className="absolute inset-0 blur-xl bg-blue-400/20 rounded-full" />
          </div>
          <p className="text-cream text-sm mb-1">尚未生成 AI 方案</p>
          <p className="text-creamDim text-xs mb-4">点击右上角「生成方案」让 DeepSeek 为你定制浇水计划</p>
          {config && (
            <div className="flex items-center gap-2 text-creamDim text-xs">
              <Leaf size={12} /> {config.plantName || "未填写植物"} · {config.locationName || "未填写地域"}
            </div>
          )}
        </div>
      ) : (
        <>
          {/* 统计条 */}
          <div className="grid grid-cols-3 gap-4 mb-6">
            <div className="glass rounded-2xl p-4 text-center">
              <div className="font-display text-2xl text-cream font-semibold">{sorted.length}</div>
              <div className="text-creamDim text-xs mt-1">时段数</div>
            </div>
            <div className="glass rounded-2xl p-4 text-center">
              <div className="font-display text-2xl text-blue-400 font-semibold">{totalDuration}ml</div>
              <div className="text-creamDim text-xs mt-1">总浇水量</div>
            </div>
            <div className="glass rounded-2xl p-4 text-center">
              <div className="font-display text-2xl text-blue-400 font-semibold">{sorted[0]?.waterTime?.substring(0, 5) ?? "—"}</div>
              <div className="text-creamDim text-xs mt-1">首次浇水</div>
            </div>
          </div>

          {/* AI 水印提示 */}
          <div className="flex items-center gap-2 mb-4 px-2">
            <Sparkles size={14} className="text-blue-400" />
            <span className="text-creamDim text-xs font-body">由 DeepSeek 生成的适配基准方案</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {sorted.map((item, idx) => (
              <PlanItemCard key={item.id ?? idx} item={item} index={idx} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
