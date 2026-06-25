import { useStore } from "@/store";
import { CheckCircle2, XCircle, Info, X } from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";

export default function Toast() {
  const { toasts, removeToast } = useStore();

  return (
    <div className="fixed top-6 right-6 z-50 flex flex-col gap-3">
      <AnimatePresence>
        {toasts.map((t) => {
          const Icon = t.type === "success" ? CheckCircle2 : t.type === "error" ? XCircle : Info;
          const color =
            t.type === "success" ? "text-blue-400 border-blue-500/30" : t.type === "error" ? "text-rust border-rust/30" : "text-blue-400 border-blue-500/30";
          return (
            <motion.div
              key={t.id}
              initial={{ opacity: 0, x: 40, scale: 0.95 }}
              animate={{ opacity: 1, x: 0, scale: 1 }}
              exit={{ opacity: 0, x: 40, scale: 0.9 }}
              className={`glass flex items-center gap-3 rounded-2xl px-4 py-3 pr-10 max-w-sm relative ${color}`}
            >
              <Icon size={18} />
              <span className="text-sm text-cream">{t.message}</span>
              <button
                onClick={() => removeToast(t.id)}
                className="absolute right-2 top-2 text-creamDim hover:text-cream transition-colors"
              >
                <X size={14} />
              </button>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}
