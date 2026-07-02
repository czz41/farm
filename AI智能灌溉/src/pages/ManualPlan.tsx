import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Hand, Plus, Clock, Droplets, Info } from "lucide-react";
import PageHeader from "@/components/PageHeader";
import PlanItemCard from "@/components/PlanItemCard";
import ConfirmDialog from "@/components/ConfirmDialog";
import { useStore } from "@/store";
import { updateConfig } from "@/api/config";
import { getManualPlan, addPlanItem, deletePlanItem, updatePlanItem, listPlanItems } from "@/api/plan";
import type { PlanItem } from "@/types";
import { sortItemsByTime } from "@/utils/format";
import { cn } from "@/lib/utils";

export default function ManualPlan() {
  const { pushToast, loadConfig } = useStore();
  const [items, setItems] = useState<PlanItem[]>([]);
  const [planId, setPlanId] = useState<number>(1);
  const [loading, setLoading] = useState(true);

  // 新增/编辑表单
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [time, setTime] = useState("08:00");
  const [waterDuration, setWaterDuration] = useState(10);
  const [saving, setSaving] = useState(false);

  // 删除确认
  const [deleteId, setDeleteId] = useState<number | null>(null);

  const load = async () => {
    try {
      const plan = await getManualPlan();
      const pid = plan?.id ?? 1;
      setPlanId(pid);
      // 单独获取时段明细
      const planItems = await listPlanItems(pid, 1);
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

  const handleSave = async () => {
    if (!time) {
      pushToast("error", "请选择时间");
      return;
    }
    const waterTime = time.length <= 5 ? `${time}:00` : time;
    // 校验时间是否与已有时段重复（编辑时排除自身）
    const duplicate = items.find((it) => {
      const existTime = it.waterTime ?? "";
      return existTime.substring(0, 5) === time && it.id !== editId;
    });
    if (duplicate) {
      pushToast("error", `该时间已存在浇水时段（${duplicate.waterDuration}ml），请选择其他时间`);
      return;
    }
    setSaving(true);
    try {
      if (editId !== null) {
        await updatePlanItem({ id: editId, waterTime, waterDuration, sort: items.length + 1, enable: 1 });
      } else {
        await addPlanItem({ parentId: planId, parentType: 1, waterTime, waterDuration, sort: items.length + 1, enable: 1 });
      }
      pushToast("success", "时段已保存并下发");
      setShowForm(false);
      setEditId(null);
      await load();
      // 保存后自动下发人工方案
      try {
        await updateConfig({ currentPlanType: 1 });
        await loadConfig();
      } catch {
        pushToast("info", "已保存，下发失败请稍后手动下发");
      }
    } catch (e) {
      pushToast("error", (e as Error).message || "保存失败");
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (item: PlanItem) => {
    setEditId(item.id ?? null);
    const t = item.waterTime ?? "08:00:00";
    setTime(t.substring(0, 5));
    setWaterDuration(item.waterDuration ?? 10);
    setShowForm(true);
  };

  const handleDelete = async () => {
    if (deleteId === null) return;
    try {
      await deletePlanItem(deleteId);
      pushToast("success", "时段已删除并下发");
      await load();
      // 删除后自动下发人工方案
      try {
        await updateConfig({ currentPlanType: 1 });
        await loadConfig();
      } catch {
        pushToast("info", "已删除，下发失败请稍后手动下发");
      }
    } catch (e) {
      pushToast("error", (e as Error).message || "删除失败");
    } finally {
      setDeleteId(null);
    }
  };

  const sorted = sortItemsByTime(items);
  const totalDuration = items.reduce((s, i) => s + (i.waterDuration ?? 0), 0);

  return (
    <div>
      <PageHeader
        title="人工方案"
        subtitle="用户自主掌控 · 永久人工基准 · 系统不会自动覆盖"
        icon={<Hand size={22} />}
        actions={
          <button
            onClick={() => { setEditId(null); setTime("08:00"); setWaterDuration(10); setShowForm(true); }}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl border border-blue-400/30 text-blue-400 hover:bg-blue-400/10 transition-colors text-sm font-body"
          >
            <Plus size={16} /> 新增时段
          </button>
        }
      />

      {/* 提示横幅 */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="glass rounded-2xl p-4 mb-6 flex items-start gap-3 border-blue-400/10"
      >
        <Info size={16} className="text-blue-400 mt-0.5 shrink-0" />
        <p className="text-creamDim text-xs font-body leading-relaxed">
          人工模式下，用户自主配置浇水时段与时长，方案为永久人工基准，系统不会自动覆盖。
          极端天气时（开启自动介入），AI 仅生成推荐临时方案供展示，<span className="text-cream">不会自动下发</span>，
          需在「临时方案」页手动确认切换；气象恢复后发送邮件提示切回，<span className="text-cream">无自动替换</span>。
        </p>
      </motion.div>

      {/* 统计条 */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="glass rounded-2xl p-4 text-center">
          <div className="font-display text-2xl text-cream font-semibold">{loading ? "—" : items.length}</div>
          <div className="text-creamDim text-xs mt-1">时段数</div>
        </div>
        <div className="glass rounded-2xl p-4 text-center">
          <div className="font-display text-2xl text-blue-400 font-semibold">{loading ? "—" : `${totalDuration}ml`}</div>
          <div className="text-creamDim text-xs mt-1">总浇水量</div>
        </div>
        <div className="glass rounded-2xl p-4 text-center">
          <div className="font-display text-2xl text-blue-400 font-semibold">{loading ? "—" : (sorted[0]?.waterTime?.substring(0, 5) ?? "—")}</div>
          <div className="text-creamDim text-xs mt-1">首次浇水</div>
        </div>
      </div>

      {/* 时段列表 */}
      {loading ? (
        <div className="text-creamDim text-center py-12">加载中…</div>
      ) : sorted.length === 0 && !showForm ? (
        <div className="glass rounded-3xl py-16 flex flex-col items-center text-center">
          <Droplets size={40} className="text-ash mb-4" />
          <p className="text-cream text-sm mb-1">暂无浇水时段</p>
          <p className="text-creamDim text-xs mb-4">点击右上角「新增时段」开始配置</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {sorted.map((item, idx) => (
            <PlanItemCard
              key={item.id ?? idx}
              item={item}
              index={idx}
              editable
              onEdit={() => handleEdit(item)}
              onDelete={() => setDeleteId(item.id ?? null)}
            />
          ))}
        </div>
      )}

      {/* 新增/编辑表单弹层 */}
      {showForm && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
          onClick={() => setShowForm(false)}
        >
          <motion.div
            initial={{ scale: 0.92, y: 12 }}
            animate={{ scale: 1, y: 0 }}
            className="glass rounded-3xl p-7 max-w-md w-full"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="font-display text-xl text-cream mb-6">{editId !== null ? "编辑时段" : "新增时段"}</h3>
            <div className="space-y-5">
              <div>
                <label className="block text-creamDim text-xs mb-2 flex items-center gap-1.5">
                  <Clock size={12} /> 浇水时间
                </label>
                <input
                  type="time"
                  value={time}
                  onChange={(e) => setTime(e.target.value)}
                  className="w-full bg-cream/5 border border-cream/10 rounded-xl px-4 py-3 text-cream font-mono text-lg outline-none focus:border-blue-400/40"
                />
              </div>
              <div>
                <label className="block text-creamDim text-xs mb-2 flex items-center gap-1.5">
                  <Droplets size={12} /> 单次浇水量 (ml)
                </label>
                <input
                  type="number"
                  value={waterDuration}
                  onChange={(e) => setWaterDuration(Number(e.target.value))}
                  min={1}
                  max={500}
                  step={5}
                  className="w-full bg-cream/5 border border-cream/10 rounded-xl px-4 py-3 text-cream font-body outline-none focus:border-blue-400/40"
                />
                <div className="flex gap-2 mt-2">
                  {[5, 10, 15, 30].map((v) => (
                    <button
                      key={v}
                      onClick={() => setWaterDuration(v)}
                      className={cn("px-3 py-1 rounded-lg text-xs", waterDuration === v ? "bg-blue-400/20 text-blue-400" : "bg-cream/5 text-creamDim")}
                    >
                      {v}ml
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <div className="flex gap-3 mt-7">
              <button
                onClick={() => setShowForm(false)}
                className="flex-1 py-2.5 rounded-xl border border-cream/10 text-creamDim hover:text-cream hover:bg-cream/5 transition-colors text-sm"
              >
                取消
              </button>
              <button
                onClick={handleSave}
                disabled={saving}
                className="flex-1 py-2.5 rounded-xl bg-blue-500/80 text-white hover:bg-blue-500 transition-colors text-sm font-medium disabled:opacity-50"
              >
                {saving ? "保存中…" : "保存"}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}

      <ConfirmDialog
        open={deleteId !== null}
        title="删除时段"
        message="确定删除该浇水时段吗？此操作不可撤销。"
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </div>
  );
}
