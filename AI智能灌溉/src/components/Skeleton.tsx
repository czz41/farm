import { cn } from "@/lib/utils";

interface SkeletonProps {
  className?: string;
  lines?: number;
}

export default function Skeleton({ className, lines = 1 }: SkeletonProps) {
  if (lines > 1) {
    return (
      <div className={cn("space-y-2", className)}>
        {Array.from({ length: lines }).map((_, i) => (
          <div key={i} className="h-4 bg-cream/5 rounded-lg animate-pulse" style={{ width: `${100 - i * 15}%` }} />
        ))}
      </div>
    );
  }
  return <div className={cn("h-4 bg-cream/5 rounded-lg animate-pulse", className)} />;
}
