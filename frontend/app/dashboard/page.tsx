"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import {
  Activity,
  ShieldAlert,
  Ban,
  Flame,
  ArrowRight,
  ShieldCheck,
  Zap,
  TrendingUp,
  AlertTriangle,
} from "lucide-react";
import { StatCard } from "@/components/ui/StatCard";
import { SeverityBadge, ActionBadge } from "@/components/ui/Badges";
import { fetchSecurityStatistics } from "@/lib/api";
import { mockStatistics } from "@/lib/mockData";
import { SecurityStatisticsDto } from "@/types";
import { formatDate, formatRelativeTime } from "@/lib/utils";

export default function DashboardPage() {
  const [stats, setStats] = useState<SecurityStatisticsDto>(mockStatistics);
  const [loading, setLoading] = useState(false);

  const loadData = async () => {
    setLoading(true);
    const data = await fetchSecurityStatistics();
    if (data) {
      setStats(data);
    }
    setLoading(false);
  };

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 15000);

    const onBackendOnline = () => {
      loadData();
    };
    const onUrlChanged = () => {
      loadData();
    };
    window.addEventListener("sentinel:backend-online", onBackendOnline);
    window.addEventListener("sentinel:api-url-changed", onUrlChanged);

    return () => {
      clearInterval(interval);
      window.removeEventListener("sentinel:backend-online", onBackendOnline);
      window.removeEventListener("sentinel:api-url-changed", onUrlChanged);
    };
  }, []);

  const totalReq = stats?.totalRequests || 0;
  const blockedReq = stats?.blockedRequests || 0;
  const rateLimitedReq = stats?.rateLimitedRequests || 0;
  const allowedReq = stats?.allowedRequests || 0;
  const highRiskCount =
    (stats?.eventsBySeverity?.HIGH || 0) + (stats?.eventsBySeverity?.CRITICAL || 0);

  const allowedPct = totalReq > 0 ? Math.round((allowedReq / totalReq) * 100) : 100;
  const blockedPct = totalReq > 0 ? ((blockedReq / totalReq) * 100).toFixed(1) : "0.0";
  const rateLimitedPct = totalReq > 0 ? ((rateLimitedReq / totalReq) * 100).toFixed(1) : "0.0";

  return (
    <div className="space-y-8">
      {/* Overview Banner */}
      <div className="relative overflow-hidden rounded-2xl border border-cyan-500/20 bg-gradient-to-r from-sentinel-900 via-sentinel-850 to-slate-900 p-6 backdrop-blur-xl">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-cyan-500/30 bg-cyan-500/10 px-3 py-1 text-xs font-semibold text-cyan-400">
              <span className="h-2 w-2 rounded-full bg-cyan-400 animate-ping" />
              Autonomous API Sentinel Active
            </div>
            <h1 className="mt-2 text-2xl font-black text-slate-100 sm:text-3xl">
              Security Operations Center
            </h1>
            <p className="mt-1 text-sm text-slate-400">
              Interception, deterministic threat scoring, and zero-trust policy enforcement across all microservices.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/simulator"
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-cyan-600 to-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-cyan-500/20 transition-all hover:from-cyan-500 hover:to-blue-500 hover:shadow-cyan-500/30"
            >
              <Zap className="h-4 w-4" />
              Launch Attack Simulator
            </Link>
          </div>
        </div>
      </div>

      {/* 4 Key Stat Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total API Requests"
          value={totalReq.toLocaleString()}
          subtitle="Intercepted across all routes"
          icon={Activity}
          variant="cyan"
          trend={{ value: "+12.4% vs last hour", isPositive: true }}
        />
        <StatCard
          title="Detected Threats"
          value={(stats?.totalSecurityEvents || 0).toLocaleString()}
          subtitle="OWASP & anomaly signatures"
          icon={ShieldAlert}
          variant="amber"
          trend={{ value: "5 active vectors", isPositive: false }}
        />
        <StatCard
          title="Blocked Requests"
          value={blockedReq.toLocaleString()}
          subtitle={`${blockedPct}% dropped at perimeter`}
          icon={Ban}
          variant="rose"
          trend={{ value: `${stats?.activeBlockedSources || 0} blocked IPs`, isPositive: false }}
        />
        <StatCard
          title="High Risk Events"
          value={highRiskCount.toLocaleString()}
          subtitle="Score >= 60 (High & Critical)"
          icon={Flame}
          variant="purple"
        />
      </div>

      {/* Request Activity & Threat Distribution Grid */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Request Activity Breakdown */}
        <div className="rounded-xl border border-slate-800 bg-sentinel-900/60 p-5 backdrop-blur-md lg:col-span-2">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <div>
              <h2 className="text-base font-semibold text-slate-100">Request Traffic Activity</h2>
              <p className="text-xs text-slate-400">Interception verdict distribution and health ratios</p>
            </div>
            <span className="text-xs font-mono text-cyan-400">LIVE TELEMETRY</span>
          </div>

          <div className="mt-5 space-y-4">
            {/* Visual Ratio Bar */}
            <div>
              <div className="mb-2 flex items-center justify-between text-xs text-slate-300">
                <span>Traffic Verdict Composition</span>
                <span className="font-mono text-slate-400">{totalReq} total evaluations</span>
              </div>
              <div className="flex h-3 w-full overflow-hidden rounded-full bg-slate-800">
                <div
                  style={{ width: `${allowedPct}%` }}
                  className="bg-emerald-500 transition-all duration-500"
                  title={`Allowed: ${allowedPct}%`}
                />
                <div
                  style={{ width: `${rateLimitedPct}%` }}
                  className="bg-purple-500 transition-all duration-500"
                  title={`Rate Limited: ${rateLimitedPct}%`}
                />
                <div
                  style={{ width: `${blockedPct}%` }}
                  className="bg-rose-500 transition-all duration-500"
                  title={`Blocked: ${blockedPct}%`}
                />
              </div>
            </div>

            {/* Metrics Breakdown */}
            <div className="grid grid-cols-3 gap-3 pt-2">
              <div className="rounded-lg border border-emerald-500/20 bg-emerald-500/5 p-3">
                <div className="flex items-center gap-1.5 text-xs text-emerald-400 font-medium">
                  <ShieldCheck className="h-4 w-4" />
                  Allowed Traffic
                </div>
                <div className="mt-1 text-xl font-bold text-slate-100">
                  {allowedReq.toLocaleString()}
                </div>
                <div className="text-[11px] text-slate-400">{allowedPct}% clean</div>
              </div>

              <div className="rounded-lg border border-purple-500/20 bg-purple-500/5 p-3">
                <div className="flex items-center gap-1.5 text-xs text-purple-400 font-medium">
                  <TrendingUp className="h-4 w-4" />
                  Rate Limited
                </div>
                <div className="mt-1 text-xl font-bold text-slate-100">
                  {rateLimitedReq.toLocaleString()}
                </div>
                <div className="text-[11px] text-slate-400">{rateLimitedPct}% throttled</div>
              </div>

              <div className="rounded-lg border border-rose-500/20 bg-rose-500/5 p-3">
                <div className="flex items-center gap-1.5 text-xs text-rose-400 font-medium">
                  <Ban className="h-4 w-4" />
                  Terminated / Blocked
                </div>
                <div className="mt-1 text-xl font-bold text-slate-100">
                  {blockedReq.toLocaleString()}
                </div>
                <div className="text-[11px] text-slate-400">{blockedPct}% 403 Forbidden</div>
              </div>
            </div>

            {/* Average Threat Score Banner */}
            <div className="flex items-center justify-between rounded-lg border border-slate-800 bg-sentinel-950/60 px-4 py-3">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-cyan-500/10 text-cyan-400">
                  <AlertTriangle className="h-5 w-5" />
                </div>
                <div>
                  <div className="text-xs font-medium text-slate-300">
                    Average Risk Score:{" "}
                    <span className="font-bold text-cyan-400">
                      {stats?.averageThreatScore?.toFixed(1) || "0.0"} / 100
                    </span>
                  </div>
                  <div className="text-[11px] text-slate-500">
                    Weighted across injection, auth abuse, rate limits, and enumeration
                  </div>
                </div>
              </div>
              <Link
                href="/requests"
                className="text-xs font-medium text-cyan-400 hover:text-cyan-300 hover:underline"
              >
                Inspect Telemetry &rarr;
              </Link>
            </div>
          </div>
        </div>

        {/* Severity & Threat Type Distribution */}
        <div className="rounded-xl border border-slate-800 bg-sentinel-900/60 p-5 backdrop-blur-md">
          <div className="border-b border-slate-800 pb-3">
            <h2 className="text-base font-semibold text-slate-100">Threat Signatures</h2>
            <p className="text-xs text-slate-400">Detected attacks grouped by vector</p>
          </div>

          <div className="mt-4 space-y-3">
            {Object.entries(stats?.eventsByThreatType || {}).length === 0 ? (
              <p className="py-6 text-center text-xs text-slate-500">No threat signatures detected yet.</p>
            ) : (
              Object.entries(stats?.eventsByThreatType || {}).map(([type, count]) => {
                const totalEvents = stats?.totalSecurityEvents || 1;
                const pct = Math.round((count / totalEvents) * 100);
                return (
                  <div key={type} className="space-y-1">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-mono font-medium text-slate-300">
                        {type.replace(/_/g, " ")}
                      </span>
                      <span className="font-mono text-slate-400">{count} ({pct}%)</span>
                    </div>
                    <div className="h-2 w-full overflow-hidden rounded-full bg-slate-800">
                      <div
                        style={{ width: `${Math.max(8, pct)}%` }}
                        className={`h-full ${
                          type.includes("INJECTION")
                            ? "bg-rose-500"
                            : type.includes("AUTH")
                            ? "bg-amber-500"
                            : type.includes("RATE")
                            ? "bg-purple-500"
                            : "bg-cyan-500"
                        }`}
                      />
                    </div>
                  </div>
                );
              })
            )}

            {/* Severity Badges Summary */}
            <div className="mt-6 border-t border-slate-800 pt-4">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Severity Breakdown
              </span>
              <div className="mt-2 grid grid-cols-2 gap-2">
                <div className="flex items-center justify-between rounded border border-rose-500/20 bg-rose-500/5 px-2.5 py-1.5 text-xs">
                  <span className="text-rose-400 font-semibold">CRITICAL</span>
                  <span className="font-mono font-bold text-slate-200">
                    {stats?.eventsBySeverity?.CRITICAL || 0}
                  </span>
                </div>
                <div className="flex items-center justify-between rounded border border-amber-500/20 bg-amber-500/5 px-2.5 py-1.5 text-xs">
                  <span className="text-amber-400 font-semibold">HIGH</span>
                  <span className="font-mono font-bold text-slate-200">
                    {stats?.eventsBySeverity?.HIGH || 0}
                  </span>
                </div>
                <div className="flex items-center justify-between rounded border border-yellow-500/20 bg-yellow-500/5 px-2.5 py-1.5 text-xs">
                  <span className="text-yellow-400 font-semibold">MEDIUM</span>
                  <span className="font-mono font-bold text-slate-200">
                    {stats?.eventsBySeverity?.MEDIUM || 0}
                  </span>
                </div>
                <div className="flex items-center justify-between rounded border border-cyan-500/20 bg-cyan-500/5 px-2.5 py-1.5 text-xs">
                  <span className="text-cyan-400 font-semibold">LOW</span>
                  <span className="font-mono font-bold text-slate-200">
                    {stats?.eventsBySeverity?.LOW || 0}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Security Events Feed */}
      <div className="rounded-xl border border-slate-800 bg-sentinel-900/60 p-5 backdrop-blur-md">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div>
            <h2 className="text-base font-semibold text-slate-100">Recent Security Events</h2>
            <p className="text-xs text-slate-400">Latest threat detections intercepted across endpoints</p>
          </div>
          <Link
            href="/threats"
            className="inline-flex items-center gap-1 text-xs font-semibold text-cyan-400 hover:text-cyan-300"
          >
            View All Incident Logs
            <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        <div className="mt-4 divide-y divide-slate-800/60">
          {(stats?.recentEvents || []).length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-500">
              No recent security events. Send a test attack via Simulator to trigger live events!
            </p>
          ) : (
            (stats?.recentEvents || []).slice(0, 5).map((ev) => (
              <div
                key={ev.id}
                className="flex flex-col gap-2 py-3 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <SeverityBadge severity={ev.severity} />
                    <span className="font-mono text-xs font-semibold text-slate-200">
                      {ev.threatType}
                    </span>
                    <span className="font-mono text-xs text-slate-500">{ev.endpoint}</span>
                  </div>
                  <p className="text-xs text-slate-400 line-clamp-1">{ev.reason}</p>
                </div>

                <div className="flex items-center gap-3 self-end sm:self-center">
                  <span className="font-mono text-xs font-bold text-cyan-400">
                    Score: {ev.threatScore}
                  </span>
                  <ActionBadge action={ev.actionTaken} />
                  <span className="text-[11px] text-slate-500 whitespace-nowrap" title={formatDate(ev.timestamp)}>
                    {formatRelativeTime(ev.timestamp)}
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
