import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { AlertTriangle, ChevronLeft, ChevronRight, Eye, ShieldOff, Ban } from "lucide-react";
import PageHeader from "@/components/PageHeader";
import Skeleton from "@/components/Skeleton";
import ConfirmDialog from "@/components/ConfirmDialog";
import { listWarnHistoryPage, manualCancel, dismissWarn } from "@/api/warn";
import { useStore } from "@/store";
import type { WarnHistory } from "@/types";
import { formatTimestamp, warnLevelBg } from "@/utils/format";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 8;

export default function WarnHistory() {
  const { config, pushToast, loadConfig } = useStore();
  const [records, setRecords] = useState<WarnHistory[]>([]);
  const [total, setTotal] = useState(0);
  const [current, setCurrent] = useState(1);
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<WarnHistory | null>(null);
  const [cancelId, setCancelId] = useState<number | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [dismissId, setDismissId] = useState<number | null>(null);
  const [dismissing, setDismissing] = useState(false);

  const load = async (page: number) => {
    setLoading(true);
    try {
      const res = await listWarnHistoryPage(page, PAGE_SIZE);
      setRecords(res?.records ?? []);
      setTotal(res?.total ?? 0);
      setCurrent(page);
    } catch {
      setRecords([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(1);
  }, []);

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  // 当前处于临时模式时，alert 记录可手动解除
  const inTempMode = config?.currentPlanType === 3;

  const handleCancel = async () => {
    if (cancelId === null) return;
    setCancelling(true);
    try {
      await manualCancel();
      pushToast("success", "预警已手动解除，已恢复原方案");
      await loadConfig();
      await load(current);
    } catch (e) {
      pushToast("error", (e as Error).message || "解除失败");
    } finally {
      setCancelling(false);
      setCancelId(null);
    }
  };

  // 手动作废单条预警（仅标记 is_valid=0，不影响临时方案）
  const handleDismiss = async () => {
    if (dismissId === null) return;
    setDismissing(true);
    try {
      await dismissWarn(dismissId);
      pushToast("success", "预警已作废");
      await load(current);
    } catch (e) {
      pushToast("error", (e as Error).message || "作废失败");
    } finally {
      setDismissing(false);
      setDismissId(null);
    }
  };

  return (
    <div>
      <PageHeader
        title="预警历史"
        subtitle="极端天气预警记录 · alert / cancel 消息追踪"
        icon={<AlertTriangle size={22} />}
      />

      <div className="glass rounded-3xl overflow-hidden">
        {/* 表头 */}
        <div className="grid grid-cols-12 gap-2 px-6 py-3 border-b border-cream/5 text-creamDim text-xs font-body uppercase tracking-wider">
          <div className="col-span-1">等级</div>
          <div className="col-span-1">预警类型</div>
          <div className="col-span-3">预警详细描述</div>
          <div className="col-span-2">开始时间</div>
          <div className="col-span-2">结束时间</div>
          <div className="col-span-1">状态</div>
          <div className="col-span-2 text-right">操作</div>
        </div>

        {/* 表体 */}
        {loading ? (
          <div className="px-6 py-8"><Skeleton lines={6} /></div>
        ) : records.length === 0 ? (
          <div className="flex flex-col items-center py-16 text-center">
            <AlertTriangle size={36} className="text-ash mb-3" />
            <p className="text-creamDim text-sm">暂无预警记录</p>
          </div>
        ) : (
          records.map((w, idx) => {
            const isAlert = w.msgType === "alert";
            const isValid = w.isValid === 1;
            const canCancel = isAlert && isValid && inTempMode;
            const canDismiss = isAlert && isValid;
            return (
              <motion.div
                key={w.id ?? idx}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: idx * 0.03 }}
                className={cn(
                  "grid grid-cols-12 gap-2 px-6 py-3.5 border-b border-cream/5 last:border-0 hover:bg-cream/[0.02] transition-colors items-center",
                  !isValid && "opacity-60"
                )}
              >
                <div className="col-span-1">
                  <span className={cn("text-[10px] px-2 py-0.5 rounded-full border font-medium", warnLevelBg(w.warnLevel))}>
                    {w.warnLevel || "未知"}
                  </span>
                </div>
                <div className="col-span-1 text-cream text-sm font-body truncate">{w.warnType || "—"}</div>
                <div className="col-span-3 text-creamDim text-xs font-body truncate" title={w.descText || ""}>
                  {w.descText || "—"}
                </div>
                <div className="col-span-2 text-creamDim text-xs font-mono truncate">{formatTimestamp(w.alertStart)}</div>
                <div className="col-span-2 text-creamDim text-xs font-mono truncate">{formatTimestamp(w.alertEnd)}</div>
                <div className="col-span-1">
                  <span className={cn(
                    "text-[10px] px-2 py-0.5 rounded-full font-medium",
                    isValid ? "bg-emerald-500/15 text-emerald-400" : "bg-ash/20 text-creamDim"
                  )}>
                    {isValid ? "有效" : "已作废"}
                  </span>
                </div>
                <div className="col-span-2 flex items-center justify-end gap-2">
                  <button
                    onClick={() => setDetail(w)}
                    className="inline-flex items-center gap-1 text-creamDim hover:text-blue-400 transition-colors text-xs"
                  >
                    <Eye size={13} /> 详情
                  </button>
                  {canCancel && (
                    <button
                      onClick={() => setCancelId(w.id ?? null)}
                      className="inline-flex items-center gap-1 text-amber hover:text-rust transition-colors text-xs"
                    >
                      <ShieldOff size={13} /> 解除
                    </button>
                  )}
                  {canDismiss && (
                    <button
                      onClick={() => setDismissId(w.id ?? null)}
                      className="inline-flex items-center gap-1 text-creamDim hover:text-rust transition-colors text-xs"
                    >
                      <Ban size={13} /> 作废
                    </button>
                  )}
                </div>
              </motion.div>
            );
          })
        )}
      </div>

      {/* 分页 */}
      {total > 0 && (
        <div className="flex items-center justify-between mt-5">
          <span className="text-creamDim text-xs font-body">
            共 {total} 条 · 第 {current} / {totalPages} 页
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => load(current - 1)}
              disabled={current <= 1}
              className="p-2 rounded-lg border border-cream/10 text-creamDim hover:text-cream hover:bg-cream/5 transition-colors disabled:opacity-30"
            >
              <ChevronLeft size={16} />
            </button>
            <button
              onClick={() => load(current + 1)}
              disabled={current >= totalPages}
              className="p-2 rounded-lg border border-cream/10 text-creamDim hover:text-cream hover:bg-cream/5 transition-colors disabled:opacity-30"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}

      {/* 详情弹窗 */}
      {detail && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
          onClick={() => setDetail(null)}
        >
          <motion.div
            initial={{ scale: 0.92, y: 12 }}
            animate={{ scale: 1, y: 0 }}
            className="glass rounded-3xl p-7 max-w-lg w-full"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-3 mb-5">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-amber/15 text-amber">
                <AlertTriangle size={20} />
              </div>
              <div>
                <h3 className="font-display text-xl text-cream">{detail.warnType || "预警详情"}</h3>
                <span className={cn("text-[10px] px-2 py-0.5 rounded-full border font-medium", warnLevelBg(detail.warnLevel))}>
                  {detail.warnLevel || "未知"}
                </span>
              </div>
            </div>
            <div className="space-y-3 text-sm">
              <Row label="消息类型" value={detail.msgType === "alert" ? "预警 (alert)" : "解除 (cancel)"} />
              <Row label="是否有效" value={detail.isValid === 1 ? "有效" : "已作废"} />
              <Row label="预警ID" value={detail.warnId || "—"} mono />
              <Row label="开始时间" value={formatTimestamp(detail.alertStart)} mono />
              <Row label="结束时间" value={formatTimestamp(detail.alertEnd)} mono />
              <Row label="记录时间" value={formatTimestamp(detail.recordTime)} mono />
              <div>
                <div className="text-creamDim text-xs mb-1.5">预警描述</div>
                <div className="glass rounded-xl p-4 text-cream text-sm font-body leading-relaxed max-h-40 overflow-y-auto">
                  {detail.descText || "无描述"}
                </div>
              </div>
            </div>
            <button
              onClick={() => setDetail(null)}
              className="w-full mt-6 py-2.5 rounded-xl border border-cream/10 text-creamDim hover:text-cream hover:bg-cream/5 transition-colors text-sm"
            >
              关闭
            </button>
          </motion.div>
        </motion.div>
      )}

      <ConfirmDialog
        open={cancelId !== null}
        title="手动解除预警"
        message="确认手动解除当前预警吗？将作废生效中的临时方案并恢复原浇水方案，同时发送邮件通知。"
        confirmText={cancelling ? "解除中…" : "确认解除"}
        onConfirm={handleCancel}
        onCancel={() => setCancelId(null)}
      />

      <ConfirmDialog
        open={dismissId !== null}
        title="作废预警记录"
        message="确认作废该预警记录吗？作废后该记录将标记为已作废，不影响当前执行的临时方案。"
        confirmText={dismissing ? "作废中…" : "确认作废"}
        onConfirm={handleDismiss}
        onCancel={() => setDismissId(null)}
      />
    </div>
  );
}

function Row({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-creamDim text-xs">{label}</span>
      <span className={cn("text-cream text-sm text-right", mono && "font-mono")}>{value}</span>
    </div>
  );
}
