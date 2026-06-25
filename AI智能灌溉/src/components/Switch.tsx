import { cn } from "@/lib/utils";

interface SwitchProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
}

export default function Switch({ checked, onChange, disabled }: SwitchProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      onClick={() => !disabled && onChange(!checked)}
      className={cn(
        "relative inline-flex h-7 w-12 items-center rounded-full transition-colors duration-300 outline-none",
        checked ? "bg-blue-500/80 shadow-blue-500/25" : "bg-ash/30",
        disabled && "opacity-40 cursor-not-allowed"
      )}
    >
      <span
        className={cn(
          "inline-block h-5 w-5 rounded-full bg-cream shadow-md transition-transform duration-300",
          checked ? "translate-x-6" : "translate-x-1"
        )}
      />
    </button>
  );
}
