import { useStore } from "@/store";
import { usePolling } from "@/hooks/usePolling";
import { timeAgo } from "@/utils/format";
import { cn } from "@/lib/utils";

export default function DeviceStatusBadge({ compact = false }: { compact?: boolean }) {
  const { device, loadDevice } = useStore();
  usePolling(loadDevice, 15000);

  const online = device?.online ?? false;

  if (compact) {
    return (
      <div className="flex items-center gap-2">
        <span className="relative flex h-2.5 w-2.5">
          {online && <span className="absolute inline-flex h-full w-full rounded-full bg-blue-500 opacity-75 animate-pulse-ring" />}
          <span className={cn("relative inline-flex h-2.5 w-2.5 rounded-full", online ? "bg-blue-500" : "bg-rust")} />
        </span>
        <span className={cn("text-xs font-medium", online ? "text-blue-400" : "text-rust")}>
          {online ? "在线" : "离线"}
        </span>
      </div>
    );
  }

  return (
    <div className="glass rounded-2xl p-5">
      <div className="flex items-center justify-between mb-4">
        <span className="text-creamDim text-sm font-body">设备状态</span>
        <span className="relative flex h-3 w-3">
          {online && <span className="absolute inline-flex h-full w-full rounded-full bg-blue-500 opacity-75 animate-pulse-ring" />}
          <span className={cn("relative inline-flex h-3 w-3 rounded-full", online ? "bg-blue-500 animate-breathe" : "bg-rust")} />
        </span>
      </div>
      <div className={cn("font-display text-2xl mb-3", online ? "text-blue-400" : "text-rust")}>
        {online ? "在线" : "离线"}
      </div>
      <div className="space-y-1.5 text-xs">
        <div className="flex justify-between">
          <span className="text-creamDim">最后心跳</span>
          <span className="text-cream font-mono">{timeAgo(device?.lastSeen)}</span>
        </div>
        {device?.lastPayload && (
          <div className="flex justify-between gap-2">
            <span className="text-creamDim shrink-0">上报数据</span>
            <span className="text-creamDim font-mono truncate max-w-[160px]">{device.lastPayload}</span>
          </div>
        )}
      </div>
    </div>
  );
}
