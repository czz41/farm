import { useEffect, useRef } from "react";

// 轮询 hook：每隔 intervalMs 执行一次 callback
export function usePolling(callback: () => void, intervalMs: number, enabled = true) {
  const savedCallback = useRef(callback);
  savedCallback.current = callback;

  useEffect(() => {
    if (!enabled) return;
    const timer = setInterval(() => savedCallback.current(), intervalMs);
    return () => clearInterval(timer);
  }, [intervalMs, enabled]);
}
