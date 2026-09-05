"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  ShieldAlert,
  Server,
  Activity,
  Sliders,
  Flame,
  ShieldCheck,
} from "lucide-react";

const navItems = [
  {
    label: "Dashboard",
    href: "/dashboard",
    icon: LayoutDashboard,
  },
  {
    label: "Threats",
    href: "/threats",
    icon: ShieldAlert,
  },
  {
    label: "Endpoints",
    href: "/endpoints",
    icon: Server,
  },
  {
    label: "Requests",
    href: "/requests",
    icon: Activity,
  },
  {
    label: "Policies",
    href: "/policies",
    icon: Sliders,
  },
  {
    label: "Simulator",
    href: "/simulator",
    icon: Flame,
  },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-800 bg-sentinel-950/95 backdrop-blur-xl">
      {/* Brand Header */}
      <div className="flex h-16 items-center gap-3 border-b border-slate-800/80 px-6">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-cyan-600 to-blue-500 text-white shadow-lg shadow-cyan-500/20">
          <ShieldCheck className="h-6 w-6" />
        </div>
        <div>
          <h1 className="text-sm font-black tracking-wider text-slate-100 uppercase">
            API SENTINEL
          </h1>
          <p className="text-[10px] font-medium tracking-widest text-cyan-400 uppercase">
            Runtime Security
          </p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1.5 px-3 py-4">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive =
            pathname === item.href || (item.href !== "/" && pathname?.startsWith(item.href));

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all ${
                isActive
                  ? "border border-cyan-500/30 bg-cyan-500/10 text-cyan-400 shadow-[0_0_15px_rgba(6,182,212,0.1)]"
                  : "text-slate-400 hover:bg-slate-900 hover:text-slate-200"
              }`}
            >
              <Icon
                className={`h-5 w-5 transition-colors ${
                  isActive ? "text-cyan-400" : "text-slate-500 group-hover:text-slate-300"
                }`}
              />
              <span>{item.label}</span>
              {isActive && (
                <span className="ml-auto h-1.5 w-1.5 rounded-full bg-cyan-400 shadow-[0_0_8px_#06b6d4]" />
              )}
            </Link>
          );
        })}
      </nav>

      {/* Footer Info */}
      <div className="border-t border-slate-800/80 p-4">
        <div className="rounded-lg border border-slate-800 bg-sentinel-900/60 p-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-300">Defense Engine</span>
            <span className="flex h-2 w-2 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
          </div>
          <p className="mt-1 text-[11px] text-slate-500">
            Real-time heuristic & deterministic protection active.
          </p>
        </div>
      </div>
    </aside>
  );
}
