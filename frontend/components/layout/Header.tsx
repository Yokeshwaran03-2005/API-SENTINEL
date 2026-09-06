"use client";

import React, { useEffect, useState, useRef } from "react";
import { usePathname } from "next/navigation";
import {
  RefreshCw,
  Server,
  ShieldCheck,
  AlertCircle,
  Activity,
  Check,
  ChevronDown,
  ExternalLink,
  Zap,
} from "lucide-react";
import {
  checkBackendHealth,
  wakeUpBackend,
  getApiBaseUrl,
  setApiBaseUrl,
  resetApiBaseUrl,
  startKeepAlivePing,
  HealthStatus,
} from "@/lib/api";

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

const RENDER_CLOUD_URL = "https://api-backend-wc8m.onrender.com";
const LOCALHOST_URL = "http://localhost:8080";

export function Header() {
  const pathname = usePathname() || "/dashboard";
  const [backendStatus, setBackendStatus] = useState<HealthStatus>({
    connected: false,
    statusText: "Connecting...",
    isWakingUp: true,
  });
  const [checking, setChecking] = useState(false);
  const [showUrlModal, setShowUrlModal] = useState(false);
  const [customUrlInput, setCustomUrlInput] = useState("");
  const [currentBaseUrl, setCurrentBaseUrl] = useState(getApiBaseUrl());
  const modalRef = useRef<HTMLDivElement>(null);

  const pageInfo =
    titles[pathname] || {
      title: "API Sentinel",
      subtitle: "Enterprise API Runtime Security",
    };

  // Perform standard health check or full wake-up
  const refreshStatus = async (isWakeUp = false) => {
    setChecking(true);
    if (isWakeUp) {
      setBackendStatus({
        connected: false,
        statusText: "Waking up Render backend...",
        isWakingUp: true,
      });
      const res = await wakeUpBackend((attempt, msg) => {
        setBackendStatus({
          connected: false,
          statusText: msg,
          isWakingUp: true,
        });
      });
      setBackendStatus(res);
    } else {
      const res = await checkBackendHealth(8000);
      if (!res.connected && !backendStatus.connected) {
        // If initial check failed, trigger automatic wake-up loop in background
        wakeUpBackend((attempt, msg) => {
          setBackendStatus({
            connected: false,
            statusText: msg,
            isWakingUp: true,
          });
        }).then((wakeRes) => {
          setBackendStatus(wakeRes);
        });
      } else {
        setBackendStatus(res);
      }
    }
    setChecking(false);
  };

  useEffect(() => {
    // Start keep alive ping in background
    const stopKeepAlive = startKeepAlivePing();

    // Initial check
    refreshStatus();

    // Periodic check every 30s
    const interval = setInterval(() => {
      refreshStatus(false);
    }, 30000);

    const onUrlChanged = (e: any) => {
      setCurrentBaseUrl(e.detail);
      refreshStatus(true);
    };
    window.addEventListener("sentinel:api-url-changed", onUrlChanged);

    return () => {
      stopKeepAlive();
      clearInterval(interval);
      window.removeEventListener("sentinel:api-url-changed", onUrlChanged);
    };
  }, []);

  // Close modal on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (modalRef.current && !modalRef.current.contains(e.target as Node)) {
        setShowUrlModal(false);
      }
    };
    if (showUrlModal) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [showUrlModal]);

  const handleSelectPreset = (url: string) => {
    setApiBaseUrl(url);
    setCurrentBaseUrl(url);
    setShowUrlModal(false);
  };

  const handleApplyCustomUrl = (e: React.FormEvent) => {
    e.preventDefault();
    if (customUrlInput.trim()) {
      setApiBaseUrl(customUrlInput.trim());
      setCurrentBaseUrl(customUrlInput.trim());
      setCustomUrlInput("");
      setShowUrlModal(false);
    }
  };

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-slate-800 bg-sentinel-950/80 px-6 backdrop-blur-xl">
      <div>
        <h2 className="text-base font-bold text-slate-100">{pageInfo.title}</h2>
        <p className="hidden text-xs text-slate-400 sm:block">{pageInfo.subtitle}</p>
      </div>

      <div className="relative flex items-center gap-3">
        {/* Backend Status Badge */}
        <div
          className={`flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-medium transition-all ${
            backendStatus.connected
              ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
              : backendStatus.isWakingUp
              ? "border-cyan-500/30 bg-cyan-500/10 text-cyan-300 animate-pulse"
              : "border-amber-500/30 bg-amber-500/10 text-amber-400"
          }`}
          title={`Active API: ${currentBaseUrl}${
            backendStatus.latencyMs ? ` (${backendStatus.latencyMs}ms)` : ""
          }`}
        >
          {backendStatus.connected ? (
            <>
              <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
              <span>{backendStatus.statusText}</span>
              {backendStatus.latencyMs !== undefined && (
                <span className="hidden font-mono text-[10px] text-emerald-500/80 sm:inline">
                  {backendStatus.latencyMs}ms
                </span>
              )}
            </>
          ) : backendStatus.isWakingUp ? (
            <>
              <Activity className="h-3.5 w-3.5 animate-spin text-cyan-400" />
              <span>{backendStatus.statusText}</span>
            </>
          ) : (
            <>
              <AlertCircle className="h-3.5 w-3.5 text-amber-400" />
              <span>{backendStatus.statusText}</span>
              <button
                onClick={() => refreshStatus(true)}
                className="ml-1 inline-flex items-center gap-1 rounded bg-amber-500/20 px-1.5 py-0.5 text-[10px] font-semibold text-amber-300 hover:bg-amber-500/30"
              >
                <Zap className="h-2.5 w-2.5" />
                Wake Up
              </button>
            </>
          )}
        </div>

        {/* Backend Endpoint Switcher Pill */}
        <div className="relative">
          <button
            onClick={() => setShowUrlModal(!showUrlModal)}
            className="flex items-center gap-1.5 rounded-lg border border-slate-800 bg-sentinel-900/80 px-2.5 py-1 text-xs text-slate-300 hover:border-slate-700 hover:bg-sentinel-850 transition-all"
            title="Click to switch backend endpoint (Render / Localhost)"
          >
            <Server className="h-3.5 w-3.5 text-slate-400" />
            <span className="max-w-[150px] truncate font-mono text-[11px] text-slate-200 sm:max-w-[200px]">
              {currentBaseUrl.replace(/^https?:\/\//, "")}
            </span>
            <ChevronDown className="h-3 w-3 text-slate-500" />
          </button>

          {/* Endpoint Switcher Dropdown */}
          {showUrlModal && (
            <div
              ref={modalRef}
              className="absolute right-0 top-10 z-50 w-80 rounded-xl border border-slate-800 bg-sentinel-900 p-4 shadow-2xl backdrop-blur-2xl"
            >
              <div className="mb-3 flex items-center justify-between border-b border-slate-800 pb-2">
                <span className="text-xs font-semibold text-slate-200">
                  Backend API Endpoint
                </span>
                <span
                  className={`h-2 w-2 rounded-full ${
                    backendStatus.connected
                      ? "bg-emerald-400"
                      : backendStatus.isWakingUp
                      ? "bg-cyan-400 animate-ping"
                      : "bg-amber-400"
                  }`}
                />
              </div>

              {/* Presets */}
              <div className="space-y-1.5">
                <button
                  onClick={() => handleSelectPreset(RENDER_CLOUD_URL)}
                  className={`flex w-full items-center justify-between rounded-lg px-2.5 py-2 text-left text-xs transition-colors ${
                    currentBaseUrl === RENDER_CLOUD_URL
                      ? "bg-cyan-500/10 text-cyan-300 border border-cyan-500/30"
                      : "text-slate-300 hover:bg-slate-800"
                  }`}
                >
                  <div>
                    <div className="font-medium">Render Cloud (Live)</div>
                    <div className="font-mono text-[10px] text-slate-400">
                      api-backend-wc8m.onrender.com
                    </div>
                  </div>
                  {currentBaseUrl === RENDER_CLOUD_URL && (
                    <Check className="h-4 w-4 text-cyan-400" />
                  )}
                </button>

                <button
                  onClick={() => handleSelectPreset(LOCALHOST_URL)}
                  className={`flex w-full items-center justify-between rounded-lg px-2.5 py-2 text-left text-xs transition-colors ${
                    currentBaseUrl === LOCALHOST_URL
                      ? "bg-cyan-500/10 text-cyan-300 border border-cyan-500/30"
                      : "text-slate-300 hover:bg-slate-800"
                  }`}
                >
                  <div>
                    <div className="font-medium">Localhost (Dev)</div>
                    <div className="font-mono text-[10px] text-slate-400">
                      http://localhost:8080
                    </div>
                  </div>
                  {currentBaseUrl === LOCALHOST_URL && (
                    <Check className="h-4 w-4 text-cyan-400" />
                  )}
                </button>
              </div>

              {/* Custom URL form */}
              <form onSubmit={handleApplyCustomUrl} className="mt-3 pt-2 border-t border-slate-800">
                <label className="text-[11px] font-medium text-slate-400">
                  Custom Endpoint URL
                </label>
                <div className="mt-1 flex gap-1.5">
                  <input
                    type="url"
                    placeholder="https://your-api.com"
                    value={customUrlInput}
                    onChange={(e) => setCustomUrlInput(e.target.value)}
                    className="flex-1 rounded-lg border border-slate-700 bg-sentinel-950 px-2.5 py-1 text-xs text-slate-200 placeholder-slate-500 focus:border-cyan-500 focus:outline-none"
                  />
                  <button
                    type="submit"
                    className="rounded-lg bg-cyan-600 px-2.5 py-1 text-xs font-semibold text-white hover:bg-cyan-500"
                  >
                    Set
                  </button>
                </div>
              </form>

              {/* Action buttons */}
              <div className="mt-3 flex items-center justify-between pt-2 border-t border-slate-800">
                <button
                  onClick={() => {
                    resetApiBaseUrl();
                    setCurrentBaseUrl(getApiBaseUrl());
                    setShowUrlModal(false);
                  }}
                  className="text-[11px] text-slate-400 hover:text-slate-200"
                >
                  Reset Default
                </button>
                <button
                  onClick={() => {
                    setShowUrlModal(false);
                    refreshStatus(true);
                  }}
                  className="inline-flex items-center gap-1 rounded bg-slate-800 px-2 py-1 text-[11px] font-medium text-slate-200 hover:bg-slate-700"
                >
                  <Zap className="h-3 w-3 text-cyan-400" />
                  Wake & Ping
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Refresh Button */}
        <button
          onClick={() => refreshStatus(true)}
          disabled={checking}
          className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-800 bg-sentinel-900 text-slate-400 hover:bg-slate-800 hover:text-slate-200 disabled:opacity-50 transition-all"
          title="Force reconnect & wake up backend"
        >
          <RefreshCw className={`h-3.5 w-3.5 ${checking ? "animate-spin" : ""}`} />
        </button>
      </div>
    </header>
  );
}
