import React from "react";
import { LucideIcon } from "lucide-react";

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  variant?: "cyan" | "rose" | "amber" | "emerald" | "purple" | "slate";
  trend?: {
    value: string;
    isPositive?: boolean;
  };
}

const variantStyles = {
  cyan: {
    border: "border-cyan-500/20 hover:border-cyan-500/40",
    bgIcon: "bg-cyan-500/10 text-cyan-400",
    glow: "hover:shadow-[0_0_20px_rgba(6,182,212,0.15)]",
    textVal: "text-cyan-400",
  },
  rose: {
    border: "border-rose-500/20 hover:border-rose-500/40",
    bgIcon: "bg-rose-500/10 text-rose-400",
    glow: "hover:shadow-[0_0_20px_rgba(244,63,94,0.15)]",
    textVal: "text-rose-400",
  },
  amber: {
    border: "border-amber-500/20 hover:border-amber-500/40",
    bgIcon: "bg-amber-500/10 text-amber-400",
    glow: "hover:shadow-[0_0_20px_rgba(245,158,11,0.15)]",
    textVal: "text-amber-400",
  },
  emerald: {
    border: "border-emerald-500/20 hover:border-emerald-500/40",
    bgIcon: "bg-emerald-500/10 text-emerald-400",
    glow: "hover:shadow-[0_0_20px_rgba(16,185,129,0.15)]",
    textVal: "text-emerald-400",
  },
  purple: {
    border: "border-purple-500/20 hover:border-purple-500/40",
    bgIcon: "bg-purple-500/10 text-purple-400",
    glow: "hover:shadow-[0_0_20px_rgba(168,85,247,0.15)]",
    textVal: "text-purple-400",
  },
  slate: {
    border: "border-slate-800 hover:border-slate-700",
    bgIcon: "bg-slate-800 text-slate-400",
    glow: "",
    textVal: "text-slate-100",
  },
};

export function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  variant = "cyan",
  trend,
}: StatCardProps) {
  const styles = variantStyles[variant];

  return (
    <div
      className={`relative overflow-hidden rounded-xl border bg-sentinel-900/80 p-5 backdrop-blur-md transition-all duration-200 ${styles.border} ${styles.glow}`}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
            {title}
          </p>
          <div className="mt-2 flex items-baseline gap-2">
            <h3 className={`text-2xl sm:text-3xl font-bold tracking-tight ${styles.textVal}`}>
              {value}
            </h3>
            {trend && (
              <span
                className={`text-xs font-semibold ${
                  trend.isPositive ? "text-emerald-400" : "text-rose-400"
                }`}
              >
                {trend.value}
              </span>
            )}
          </div>
          {subtitle && (
            <p className="mt-1 text-xs text-slate-500">{subtitle}</p>
          )}
        </div>
        <div className={`rounded-lg p-2.5 ${styles.bgIcon}`}>
          <Icon className="h-6 w-6" />
        </div>
      </div>
      <div className="absolute inset-x-0 bottom-0 h-0.5 bg-gradient-to-r from-transparent via-slate-700/50 to-transparent" />
    </div>
  );
}
