import { create } from "zustand";
import type { SysConfig, DeviceStatusVO } from "@/types";
import { getConfig } from "@/api/config";
import { getDeviceStatus } from "@/api/device";

interface ToastItem {
  id: number;
  type: "success" | "error" | "info";
  message: string;
}

interface AppState {
  config: SysConfig | null;
  configLoading: boolean;
  device: DeviceStatusVO | null;
  toasts: ToastItem[];
  // actions
  loadConfig: () => Promise<void>;
  loadDevice: () => Promise<void>;
  setConfig: (c: SysConfig) => void;
  setDevice: (d: DeviceStatusVO) => void;
  pushToast: (type: ToastItem["type"], message: string) => void;
  removeToast: (id: number) => void;
}

let toastId = 0;

export const useStore = create<AppState>((set) => ({
  config: null,
  configLoading: false,
  device: null,
  toasts: [],

  loadConfig: async () => {
    set({ configLoading: true });
    try {
      const data = await getConfig();
      set({ config: data });
    } catch {
      // 配置可能尚未初始化，静默处理
    } finally {
      set({ configLoading: false });
    }
  },

  loadDevice: async () => {
    try {
      const data = await getDeviceStatus();
      set({ device: data });
    } catch {
      // 静默
    }
  },

  setConfig: (c) => set({ config: c }),
  setDevice: (d) => set({ device: d }),

  pushToast: (type, message) => {
    const id = ++toastId;
    set((s) => ({ toasts: [...s.toasts, { id, type, message }] }));
    setTimeout(() => {
      set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
    }, 3500);
  },

  removeToast: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}));
