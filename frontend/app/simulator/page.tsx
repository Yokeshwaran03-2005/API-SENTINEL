"use client";

import React, { useState } from "react";
import {
  Flame,
  Zap,
  Play,
  RotateCcw,
  ShieldAlert,
  CheckCircle2,
  AlertTriangle,
  Terminal,
} from "lucide-react";
import { SeverityBadge, ActionBadge, MethodBadge } from "@/components/ui/Badges";
import { executeSimulationStep } from "@/lib/api";
import { mockAttackScenarios } from "@/lib/mockData";
import { SimulatorScenario, SimulationResult } from "@/types";

export default function SimulatorPage() {
  const [selectedScenario, setSelectedScenario] = useState<SimulatorScenario>(
    mockAttackScenarios[0]
  );
  const [requestCount, setRequestCount] = useState<number>(5);
  const [running, setRunning] = useState<boolean>(false);
  const [progress, setProgress] = useState<number>(0);
  const [results, setResults] = useState<SimulationResult[]>([]);

  const handleLaunch = async () => {
    setRunning(true);
    setProgress(0);
    setResults([]);

    const newResults: SimulationResult[] = [];
    for (let i = 0; i < requestCount; i++) {
      const res = await executeSimulationStep(selectedScenario, i);
      newResults.push(res);
      setResults([...newResults]);
      setProgress(Math.round(((i + 1) / requestCount) * 100));
      // Artificial delay for visual streaming effect
      if (i < requestCount - 1) {
        await new Promise((resolve) => setTimeout(resolve, 350));
      }
    }

    setRunning(false);
  };

  const handleClear = () => {
    setResults([]);
    setProgress(0);
  };

  const detectedCount = results.filter((r) => r.detected).length;
  const maxScore = results.length > 0 ? Math.max(...results.map((r) => r.score)) : 0;
  const highestAction = results.some((r) => r.action === "BLOCK")
    ? "BLOCK"
    : results.some((r) => r.action === "RATE_LIMIT")
    ? "RATE_LIMIT"
    : results.some((r) => r.action === "WARN")
    ? "WARN"
    : results.some((r) => r.action === "MONITOR")
    ? "MONITOR"
    : "ALLOW";

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="relative overflow-hidden rounded-2xl border border-rose-500/20 bg-gradient-to-r from-sentinel-900 via-slate-900 to-sentinel-850 p-6 backdrop-blur-xl">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-rose-500/30 bg-rose-500/10 px-3 py-1 text-xs font-semibold text-rose-400">
              <Flame className="h-3.5 w-3.5" />
              Automated Attack Simulation Harness
            </div>
            <h1 className="mt-2 text-2xl font-black text-slate-100 sm:text-3xl">
              API Attack Simulator
            </h1>
            <p className="mt-1 text-sm text-slate-400">
              Inject synthetic OWASP vulnerability payloads, rate abuse bursts, and enumeration scans to verify real-time perimeter defense.
            </p>
          </div>
        </div>
      </div>

      {/* Attack Scenario Selection & Configuration */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Scenarios List */}
        <div className="rounded-xl border border-slate-800 bg-sentinel-900/60 p-5 backdrop-blur-md lg:col-span-2">
          <h2 className="text-base font-semibold text-slate-100">1. Select Attack Scenario</h2>
          <p className="text-xs text-slate-400">Choose an attack vector or legitimate baseline</p>

          <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
            {mockAttackScenarios.map((sc) => {
              const isSelected = selectedScenario.id === sc.id;
              return (
                <div
                  key={sc.id}
                  onClick={() => setSelectedScenario(sc)}
                  className={`cursor-pointer rounded-xl border p-4 transition-all ${
                    isSelected
                      ? "border-cyan-500/50 bg-cyan-500/10 shadow-[0_0_15px_rgba(6,182,212,0.15)]"
                      : "border-slate-800 bg-sentinel-950/60 hover:border-slate-700 hover:bg-slate-900/60"
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold text-slate-100">
                        {sc.name}
                      </span>
                    </div>
                    <SeverityBadge severity={sc.expectedSeverity} />
                  </div>
                  <p className="mt-2 text-xs text-slate-400">{sc.description}</p>
                  <div className="mt-3 flex items-center gap-2">
                    <MethodBadge method={sc.defaultMethod} />
                    <span className="font-mono text-[11px] text-slate-400 truncate">
                      {sc.defaultEndpoint}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Execution Settings & Trigger */}
        <div className="flex flex-col justify-between rounded-xl border border-slate-800 bg-sentinel-900/60 p-5 backdrop-blur-md">
          <div className="space-y-4">
            <h2 className="text-base font-semibold text-slate-100">2. Configure & Run</h2>
            <p className="text-xs text-slate-400">Adjust burst volume and target parameters</p>

            {/* Request Count Controls */}
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">
                Request Count ({requestCount} requests)
              </label>
              <div className="flex gap-2">
                {[1, 3, 5, 10, 20].map((n) => (
                  <button
                    key={n}
                    type="button"
                    onClick={() => setRequestCount(n)}
                    className={`flex-1 rounded-lg border py-1.5 text-xs font-semibold ${
                      requestCount === n
                        ? "border-cyan-500 bg-cyan-500/20 text-cyan-300"
                        : "border-slate-800 bg-sentinel-950 text-slate-400 hover:bg-slate-800"
                    }`}
                  >
                    {n}
                  </button>
                ))}
              </div>
            </div>

            {/* Target Details Preview */}
            <div className="rounded-lg border border-slate-800 bg-sentinel-950/80 p-3 text-xs space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Vector</span>
                <span className="font-mono font-bold text-slate-200">
                  {selectedScenario.threatType}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Target</span>
                <span className="font-mono text-cyan-300 truncate max-w-[170px]">
                  {selectedScenario.defaultEndpoint}
                </span>
              </div>
              {selectedScenario.samplePayload && (
                <div>
                  <span className="text-slate-500 block mb-1">Payload Sample</span>
                  <div className="font-mono text-[11px] text-rose-300 bg-slate-900 p-1.5 rounded truncate">
                    {selectedScenario.samplePayload}
                  </div>
                </div>
              )}
            </div>

            {/* Progress Bar */}
            {running && (
              <div className="space-y-1.5">
                <div className="flex justify-between text-xs text-cyan-400 font-mono">
                  <span>Executing burst traffic...</span>
                  <span>{progress}%</span>
                </div>
                <div className="h-2 w-full overflow-hidden rounded-full bg-slate-800">
                  <div
                    style={{ width: `${progress}%` }}
                    className="h-full bg-gradient-to-r from-cyan-500 to-rose-500 transition-all duration-200"
                  />
                </div>
              </div>
            )}
          </div>

          <div className="mt-6 flex flex-col gap-2">
            <button
              onClick={handleLaunch}
              disabled={running}
              className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-rose-600 via-rose-500 to-amber-500 py-3 text-sm font-bold text-white shadow-lg shadow-rose-600/20 hover:from-rose-500 hover:to-amber-400 disabled:opacity-50"
            >
              <Zap className={`h-4 w-4 ${running ? "animate-spin" : ""}`} />
              {running ? "Simulating Attack..." : "Launch Attack Simulation"}
            </button>
            {results.length > 0 && (
              <button
                onClick={handleClear}
                disabled={running}
                className="inline-flex w-full items-center justify-center gap-1.5 rounded-lg border border-slate-800 bg-sentinel-950 py-2 text-xs font-semibold text-slate-400 hover:bg-slate-800"
              >
                <RotateCcw className="h-3.5 w-3.5" />
                Clear Results
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Simulation Results Section */}
      {results.length > 0 && (
        <div className="space-y-4">
          {/* Results Metric Banner */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 rounded-xl border border-slate-800 bg-sentinel-900/80 p-4 backdrop-blur-md">
            <div>
              <span className="text-xs text-slate-500">Requests Executed</span>
              <div className="text-xl font-bold text-slate-100">{results.length}</div>
            </div>
            <div>
              <span className="text-xs text-slate-500">Detections Triggered</span>
              <div className="text-xl font-bold text-rose-400">{detectedCount}</div>
            </div>
            <div>
              <span className="text-xs text-slate-500">Peak Threat Score</span>
              <div className="text-xl font-bold text-amber-400">{maxScore} / 100</div>
            </div>
            <div>
              <span className="text-xs text-slate-500">Enforcement Action</span>
              <div className="mt-1">
                <ActionBadge action={highestAction} />
              </div>
            </div>
          </div>

          {/* Results Step Log */}
          <div className="overflow-hidden rounded-xl border border-slate-800 bg-sentinel-900/60 backdrop-blur-md">
            <div className="flex items-center justify-between border-b border-slate-800 bg-sentinel-950/70 px-4 py-3">
              <div className="flex items-center gap-2">
                <Terminal className="h-4 w-4 text-cyan-400" />
                <h3 className="text-sm font-bold text-slate-200">Execution Telemetry Feed</h3>
              </div>
              <span className="font-mono text-xs text-slate-400">
                {results.length} event(s) recorded
              </span>
            </div>

            <div className="divide-y divide-slate-800/60">
              {results.map((r, idx) => (
                <div key={r.id || idx} className="p-4 transition-colors hover:bg-slate-800/30">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-center gap-2">
                      <span className="flex h-5 w-5 items-center justify-center rounded-full bg-slate-800 font-mono text-[10px] font-bold text-slate-300">
                        #{r.requestIndex}
                      </span>
                      <MethodBadge method={r.method} />
                      <span className="font-mono text-xs font-semibold text-slate-200">
                        {r.endpoint}
                      </span>
                    </div>

                    <div className="flex items-center gap-3">
                      <span
                        className={`font-mono text-xs font-bold ${
                          r.statusCode >= 400 ? "text-rose-400" : "text-emerald-400"
                        }`}
                      >
                        HTTP {r.statusCode}
                      </span>
                      <span className="font-mono text-xs font-bold text-cyan-400">
                        Score: {r.score}
                      </span>
                      <ActionBadge action={r.action} />
                      <span className="font-mono text-xs text-slate-500">{r.latencyMs}ms</span>
                    </div>
                  </div>

                  <div className="mt-2 space-y-1 text-xs">
                    <div className="flex items-center gap-2">
                      {r.detected ? (
                        <span className="inline-flex items-center gap-1 font-semibold text-rose-400">
                          <AlertTriangle className="h-3.5 w-3.5" />
                          Detection: {r.threatType}
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 font-semibold text-emerald-400">
                          <CheckCircle2 className="h-3.5 w-3.5" />
                          Passed Inspection
                        </span>
                      )}
                      <span className="text-slate-500">•</span>
                      <span className="text-slate-300">{r.reason}</span>
                    </div>

                    {r.evidence && (
                      <div className="font-mono text-[11px] text-slate-400 bg-sentinel-950/80 px-2.5 py-1 rounded border border-slate-800/60 truncate">
                        Evidence: {r.evidence}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
