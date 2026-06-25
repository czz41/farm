import { NavLink, useLocation } from "react-router-dom";
import type { ReactNode } from "react";
import { motion } from "framer-motion";
import {
  LayoutDashboard,
  Settings,
  Hand,
  Sparkles,
  Clock4,
  AlertTriangle,
  FlaskConical,
  Droplets,
} from "lucide-react";
import { cn } from "@/lib/utils";
import DeviceStatusBadge from "./DeviceStatusBadge";

const navItems = [
  { to: "/", label: "总览", icon: LayoutDashboard },
  { to: "/config", label: "基础配置", icon: Settings },
  { to: "/manual", label: "人工方案", icon: Hand },
  { to: "/ai", label: "AI方案", icon: Sparkles },
  { to: "/temp", label: "临时方案", icon: Clock4 },
  { to: "/warn", label: "预警历史", icon: AlertTriangle },
  { to: "/test", label: "测试面板", icon: FlaskConical },
];

export default function Layout({ children }: { children: ReactNode }) {
  const location = useLocation();

  return (
    <div className="flex h-screen overflow-hidden">
      {/* 侧边栏 */}
      <aside className="hidden md:flex w-64 shrink-0 flex-col border-r border-blue-500/10 bg-bark/40 backdrop-blur-xl">
        {/* Logo */}
        <div className="flex items-center gap-3 px-6 py-6 border-b border-blue-500/10">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 to-blue-400 shadow-blue-500/25">
            <Droplets size={22} className="text-bark" />
          </div>
          <div>
            <h1 className="font-display text-lg text-cream font-semibold leading-tight">AI智能灌溉</h1>
            <p className="text-[11px] text-creamDim font-body">智能浇灌控制系统</p>
          </div>
        </div>

        {/* 导航 */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = location.pathname === item.to;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === "/"}
                className={cn(
                  "relative flex items-center gap-3 rounded-xl px-4 py-2.5 text-sm font-body transition-all duration-200",
                  active
                    ? "text-blue-400 bg-blue-500/10"
                    : "text-creamDim hover:text-cream hover:bg-cream/5"
                )}
              >
                {active && (
                  <motion.div
                    layoutId="navActive"
                    className="absolute left-0 top-1/2 -translate-y-1/2 h-6 w-1 rounded-r-full bg-blue-500"
                  />
                )}
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* 设备状态 */}
        <div className="px-4 py-4 border-t border-blue-500/10">
          <DeviceStatusBadge />
        </div>
      </aside>

      {/* 主内容区 */}
      <main className="flex-1 overflow-y-auto">
        {/* 移动端顶栏 */}
        <div className="md:hidden flex items-center justify-between px-4 py-3 border-b border-blue-500/10 bg-bark/40 backdrop-blur-xl sticky top-0 z-10">
          <div className="flex items-center gap-2">
            <Droplets size={20} className="text-blue-400" />
            <span className="font-display text-cream">AI智能灌溉</span>
          </div>
          <DeviceStatusBadge compact />
        </div>

        {/* 移动端底部导航 */}
        <div className="md:hidden fixed bottom-0 left-0 right-0 z-20 flex justify-around bg-bark/80 backdrop-blur-xl border-t border-blue-500/10 py-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = location.pathname === item.to;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === "/"}
                className={cn(
                  "flex flex-col items-center gap-1 px-2 py-1 text-[10px]",
                  active ? "text-blue-400" : "text-creamDim"
                )}
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </div>

        <div className="px-4 md:px-10 py-6 md:py-8 max-w-7xl mx-auto pb-20 md:pb-8">
          {children}
        </div>
      </main>
    </div>
  );
}
