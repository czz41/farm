import type { PlanItem } from "@/types";
import { Clock, Droplets, Trash2, Pencil } from "lucide-react";
import { isSlotActive, isSlotPassed } from "@/utils/format";
import { cn } from "@/lib/utils";
import { motion } from "framer-motion";

interface PlanItemCardProps {
  item: PlanItem;
  index?: number;
  editable?: boolean;
  onEdit?: () => void;
  onDelete?: () => void;
}

export default function PlanItemCard({ item, index = 0, editable, onEdit, onDelete }: PlanItemCardProps) {
  const timeStr = item.waterTime?.substring(0, 5) ?? "--:--";
  const duration = item.waterDuration ?? 0;
  const active = isSlotActive(timeStr);
  const passed = isSlotPassed(timeStr);

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05, duration: 0.3 }}
      className={cn(
        "glass glass-hover rounded-2xl p-5 relative overflow-hidden",
        active && "border-blue-400/50 shadow-blue-500/25",
        passed && "opacity-60"
      )}
    >
      {/* 左侧时间竖条 */}
      <div className={cn("absolute left-0 top-0 bottom-0 w-1", active ? "bg-blue-500" : passed ? "bg-ash/40" : "bg-blue-400/60")} />

      <div className="flex items-center justify-between pl-2">
        <div className="flex items-center gap-4">
          <div className={cn(
            "flex h-14 w-14 flex-col items-center justify-center rounded-2xl font-mono",
            active ? "bg-blue-500/20 text-blue-400" : passed ? "bg-ash/15 text-creamDim" : "bg-blue-400/15 text-blue-400"
          )}>
            <Clock size={14} className="mb-0.5" />
            <span className="text-sm font-semibold">{timeStr}</span>
          </div>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Droplets size={15} className="text-blue-400" />
              <span className="text-cream font-body text-lg font-semibold">{duration}ml</span>
              {active && (
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-400 font-medium">进行中</span>
              )}
            </div>
            <span className="text-creamDim text-xs font-body">单次浇水量</span>
          </div>
        </div>

        {editable && (
          <div className="flex items-center gap-2">
            <button
              onClick={onEdit}
              className="p-2 rounded-lg text-creamDim hover:text-blue-400 hover:bg-blue-400/10 transition-colors"
            >
              <Pencil size={15} />
            </button>
            <button
              onClick={onDelete}
              className="p-2 rounded-lg text-creamDim hover:text-rust hover:bg-rust/10 transition-colors"
            >
              <Trash2 size={15} />
            </button>
          </div>
        )}
      </div>
    </motion.div>
  );
}
