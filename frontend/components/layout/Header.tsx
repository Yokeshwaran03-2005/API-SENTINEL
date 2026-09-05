"use client";

import React, { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { RefreshCw, Server, ShieldCheck, AlertCircle } from "lucide-react";
import { checkBackendHealth, getApiBaseUrl } from "@/lib/api";

const titles: Record<string, { title: string; subtitle: string }> = {
  "/dashboard": {
    title: "Security Operations Center",
    subtitle: "Real-time threat monitoring and API traffic intelligence",
  },
  "/threats": {
    title: "Threat Detection Feed",
    subtitle: "Investigate anomalies, injection attempts, and abuse patterns",
  },
  "/endpoints": {
    title: "Monitored Endpoints",
    subtitle: "API inventory catalog, sensitivity tiers, and quota limits",
  },
  "/requests": {
    title: "Live Request Stream",
    subtitle: "Real-time intercepted telemetry and security decisions",
  },
  "/policies": {
    title: "Security Policies",
    subtitle: "Configure threshold scores, enforcement actions, and rate limits",
  },
  "/simulator": {
    title: "Attack Simulation Engine",
    subtitle: "Test OWASP vulnerabilities and verify defensive pipeline actions",
  },
};

export function Header() {
  const pathname = usePathname() || "/dashboard";
  const [backendStatus, setBackendStatus] = useState<{
    connected: boolean;
    statusText: string;
  }>({
    connected: false,
    statusText: "Checking...",
  });
  const [checking, setChecking] = useState(false);

  const pageInfo =
    titles[pathname] || {
      title: "API Sentinel",
      subtitle: "Enterprise API Runtime Security",
    };

  const refreshStatus = async () => {
    setChecking(true);
    const res = await checkBackendHealth();
    setBackendStatus(res);
    setChecking(false);
  };

  useEffect(() => {
    refreshStatus();
    const interval = setInterval(refreshStatus, 30000);
    return () => clearInterval(interval);
  }, []);

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-800 bg-sentinel-950/80 px-6 backdrop-blur-xl">
      <div>
        <h2 className="text-base font-bold text-slate-100">{pageInfo.title}</h2>
        <p className="hidden text-xs text-slate-400 sm:block">{pageInfo.subtitle}</p>
      </div>

      <div className="flex items-center gap-3">
        {/* Backend Status Badge */}
        <div
          className={`flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-medium ${
            backendStatus.connected
              ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
              : "border-amber-500/30 bg-amber-500/10 text-amber-400"
          }`}
          title={`Configured API URL: ${getApiBaseUrl()}`}
        >
          {backendStatus.connected ? (
            <ShieldCheck className="h-3.5 w-3.5" />
          ) : (
            <AlertCircle className="h-3.5 w-3.5" />
          )}
          <span>{backendStatus.statusText}</span>
        </div>

        {/* Backend URL info */}
        <div className="hidden items-center gap-1.5 rounded-lg border border-slate-800 bg-sentinel-900/80 px-2.5 py-1 text-xs text-slate-400 md:flex">
          <Server className="h-3.5 w-3.5 text-slate-500" />
          <span className="font-mono text-[11px] text-slate-300">
            {getApiBaseUrl().replace(/^https?:\/\//, "")}
          </span>
        </div>

        {/* Refresh Button */}
        <button
          onClick={refreshStatus}
          disabled={checking}
          className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-800 bg-sentinel-900 text-slate-400 hover:bg-slate-800 hover:text-slate-200 disabled:opacity-50"
          title="Refresh connection status"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${checking ? "animate-spin" : ""}`} />
        </button>
      </div>
    </header>
  );
}
