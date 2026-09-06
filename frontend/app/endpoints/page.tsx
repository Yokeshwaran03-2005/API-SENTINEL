"use client";

import React, { useEffect, useState, useCallback } from "react";
import { Server, Search, Filter, RefreshCw, Shield, AlertCircle } from "lucide-react";
import { DataTable, Column } from "@/components/ui/DataTable";
import { MethodBadge, SeverityBadge } from "@/components/ui/Badges";
import { Modal } from "@/components/ui/Modal";
import { fetchEndpoints, fetchEndpointById } from "@/lib/api";
import { ApiEndpointDto } from "@/types";

export default function EndpointsPage() {
  const [endpoints, setEndpoints] = useState<ApiEndpointDto[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [search, setSearch] = useState("");
  const [sensitivityLevel, setSensitivityLevel] = useState("");
  const [httpMethod, setHttpMethod] = useState("");
  const [activeFilter, setActiveFilter] = useState<string>("");

  // Detail Modal
  const [selectedEndpoint, setSelectedEndpoint] = useState<ApiEndpointDto | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const loadEndpoints = useCallback(async () => {
    setLoading(true);
    const active = activeFilter === "" ? undefined : activeFilter === "true";
    const data = await fetchEndpoints({
      search: search || undefined,
      sensitivityLevel: sensitivityLevel || undefined,
      httpMethod: httpMethod || undefined,
      active,
    });
    setEndpoints(data);
    setLoading(false);
  }, [search, sensitivityLevel, httpMethod, activeFilter]);

  useEffect(() => {
    loadEndpoints();
    const onOnline = () => loadEndpoints();
    const onUrlChanged = () => loadEndpoints();
    window.addEventListener("sentinel:backend-online", onOnline);
    window.addEventListener("sentinel:api-url-changed", onUrlChanged);
    return () => {
      window.removeEventListener("sentinel:backend-online", onOnline);
      window.removeEventListener("sentinel:api-url-changed", onUrlChanged);
    };
  }, [loadEndpoints]);

  const handleRowClick = async (ep: ApiEndpointDto) => {
    const detail = await fetchEndpointById(ep.id);
    setSelectedEndpoint(detail || ep);
    setModalOpen(true);
  };

  const columns: Column<ApiEndpointDto>[] = [
    {
      header: "Endpoint",
      accessor: (row) => (
        <div className="flex items-center gap-2">
          <MethodBadge method={row.httpMethod} />
          <span className="font-mono text-xs font-semibold text-slate-100">
            {row.pathPattern}
          </span>
        </div>
      ),
    },
    {
      header: "Request Count",
      accessor: (row) => (
        <span className="font-mono text-xs font-medium text-slate-300">
          {(row.requestCount || 0).toLocaleString()}
        </span>
      ),
    },
    {
      header: "Threat Count",
      accessor: (row) => {
        const count = row.threatCount || 0;
        return (
          <span
            className={`font-mono text-xs font-bold ${
              count > 50
                ? "text-rose-400"
                : count > 20
                ? "text-amber-400"
                : count > 0
                ? "text-yellow-400"
                : "text-emerald-400"
            }`}
          >
            {count}
          </span>
        );
      },
    },
    {
      header: "Risk Score",
      accessor: (row) => {
        const score = row.riskScore || 0;
        return (
          <div className="flex items-center gap-2">
            <span
              className={`font-mono text-xs font-bold ${
                score >= 80
                  ? "text-rose-400"
                  : score >= 60
                  ? "text-amber-400"
                  : score >= 30
                  ? "text-yellow-400"
                  : "text-cyan-400"
              }`}
            >
              {score}
            </span>
            <div className="h-1.5 w-16 overflow-hidden rounded-full bg-slate-800">
              <div
                style={{ width: `${Math.min(100, Math.max(5, score))}%` }}
                className={`h-full ${
                  score >= 80
                    ? "bg-rose-500"
                    : score >= 60
                    ? "bg-amber-500"
                    : score >= 30
                    ? "bg-yellow-500"
                    : "bg-cyan-500"
                }`}
              />
            </div>
          </div>
        );
      },
    },
    {
      header: "Policy & Tier",
      accessor: (row) => (
        <div className="flex items-center gap-2">
          <SeverityBadge severity={row.sensitivityLevel} />
          <span className="text-xs text-slate-400 font-mono">
            {row.rateLimitPerMinute ? `${row.rateLimitPerMinute} req/min` : "Default"}
          </span>
        </div>
      ),
    },
    {
      header: "Status",
      accessor: (row) => (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold ${
            row.active
              ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
              : "bg-slate-800 text-slate-400 border border-slate-700"
          }`}
        >
          {row.active ? "ACTIVE" : "INACTIVE"}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header & Filters */}
      <div className="rounded-xl border border-slate-800 bg-sentinel-900/60 p-5 backdrop-blur-md">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-xl font-bold text-slate-100">Monitored API Endpoints</h1>
            <p className="text-xs text-slate-400">
              Inventory of protected API routes, traffic volumes, risk scores, and rate limit quotas
            </p>
          </div>
          <button
            onClick={() => loadEndpoints()}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-800 bg-sentinel-950 px-3 py-2 text-xs font-medium text-slate-300 hover:bg-slate-800"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
            Refresh Endpoints
          </button>
        </div>

        {/* Filter Bar */}
        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search endpoint path or description..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 pl-9 pr-3 text-xs text-slate-200 placeholder-slate-500 focus:border-cyan-500 focus:outline-none"
            />
          </div>

          <div>
            <select
              value={sensitivityLevel}
              onChange={(e) => setSensitivityLevel(e.target.value)}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 px-3 text-xs text-slate-300 focus:border-cyan-500 focus:outline-none"
            >
              <option value="">All Sensitivity Tiers</option>
              <option value="CRITICAL">CRITICAL</option>
              <option value="HIGH">HIGH</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="LOW">LOW</option>
            </select>
          </div>

          <div>
            <select
              value={httpMethod}
              onChange={(e) => setHttpMethod(e.target.value)}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 px-3 text-xs text-slate-300 focus:border-cyan-500 focus:outline-none"
            >
              <option value="">All Methods</option>
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
              <option value="PATCH">PATCH</option>
              <option value="ANY">ANY</option>
            </select>
          </div>

          <div>
            <select
              value={activeFilter}
              onChange={(e) => setActiveFilter(e.target.value)}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 px-3 text-xs text-slate-300 focus:border-cyan-500 focus:outline-none"
            >
              <option value="">All Statuses</option>
              <option value="true">Active Only</option>
              <option value="false">Inactive Only</option>
            </select>
          </div>
        </div>
      </div>

      {/* Endpoints Table */}
      <DataTable
        columns={columns}
        data={endpoints}
        loading={loading}
        emptyMessage="No endpoints match current filter criteria."
        onRowClick={handleRowClick}
      />

      {/* Endpoint Detail Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Endpoint Configuration & Protection"
        maxWidth="lg"
      >
        {selectedEndpoint && (
          <div className="space-y-4 text-xs">
            <div className="rounded-lg border border-slate-800 bg-sentinel-950/80 p-3">
              <div className="flex items-center gap-2">
                <MethodBadge method={selectedEndpoint.httpMethod} />
                <span className="font-mono text-sm font-bold text-cyan-300">
                  {selectedEndpoint.pathPattern}
                </span>
              </div>
              <p className="mt-2 text-xs text-slate-400">{selectedEndpoint.description}</p>
            </div>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 rounded-lg border border-slate-800 bg-sentinel-950/80 p-3">
              <div>
                <span className="text-slate-500">Sensitivity</span>
                <div className="mt-0.5">
                  <SeverityBadge severity={selectedEndpoint.sensitivityLevel} />
                </div>
              </div>
              <div>
                <span className="text-slate-500">Rate Limit</span>
                <div className="font-mono font-bold text-slate-200">
                  {selectedEndpoint.rateLimitPerMinute} req/min
                </div>
              </div>
              <div>
                <span className="text-slate-500">Risk Score</span>
                <div className="font-mono font-bold text-amber-400">
                  {selectedEndpoint.riskScore || 0} / 100
                </div>
              </div>
              <div>
                <span className="text-slate-500">Total Requests</span>
                <div className="font-mono font-bold text-slate-200">
                  {(selectedEndpoint.requestCount || 0).toLocaleString()}
                </div>
              </div>
            </div>

            <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-3 space-y-2">
              <div className="flex items-center gap-2 text-cyan-400 font-semibold">
                <Shield className="h-4 w-4" />
                Active Protection Rules
              </div>
              <ul className="list-disc list-inside space-y-1 text-slate-400">
                <li>Automatic payload sanitization and SQL injection inspection.</li>
                <li>Dynamic sliding-window IP rate limiting ({selectedEndpoint.rateLimitPerMinute} req/min threshold).</li>
                <li>Audit logging and anomalous identifier scanning enabled.</li>
              </ul>
            </div>

            <div className="flex justify-end pt-3 border-t border-slate-800">
              <button
                onClick={() => setModalOpen(false)}
                className="rounded-lg bg-slate-800 px-4 py-2 font-medium text-slate-200 hover:bg-slate-700"
              >
                Close
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
