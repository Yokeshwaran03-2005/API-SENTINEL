"use client";

import React, { useEffect, useState, useCallback } from "react";
import { Activity, Search, RefreshCw, Eye, ArrowUpDown } from "lucide-react";
import { DataTable, Column } from "@/components/ui/DataTable";
import { MethodBadge, ActionBadge } from "@/components/ui/Badges";
import { Modal } from "@/components/ui/Modal";
import { fetchRequests, fetchRequestById } from "@/lib/api";
import { ApiRequestDto, ApiRequestDetailDto } from "@/types";
import { formatDate, formatRelativeTime } from "@/lib/utils";

export default function RequestsPage() {
  const [requests, setRequests] = useState<ApiRequestDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [searchPath, setSearchPath] = useState("");
  const [httpMethod, setHttpMethod] = useState("");
  const [sourceIp, setSourceIp] = useState("");
  const [verdict, setVerdict] = useState("");

  // Modal
  const [selectedRequest, setSelectedRequest] = useState<ApiRequestDetailDto | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const loadRequests = useCallback(async () => {
    setLoading(true);
    const res = await fetchRequests({
      page,
      size: 15,
      path: searchPath || undefined,
      httpMethod: httpMethod || undefined,
      sourceIp: sourceIp || undefined,
      verdict: verdict || undefined,
    });
    setRequests(res.content);
    setTotalPages(res.totalPages || 1);
    setTotalElements(res.totalElements || 0);
    setLoading(false);
  }, [page, searchPath, httpMethod, sourceIp, verdict]);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  const handleRowClick = async (req: ApiRequestDto) => {
    const detail = await fetchRequestById(req.id);
    setSelectedRequest(detail || req);
    setModalOpen(true);
  };

  const columns: Column<ApiRequestDto>[] = [
    {
      header: "Method",
      accessor: (row) => <MethodBadge method={row.httpMethod} />,
    },
    {
      header: "Endpoint",
      accessor: (row) => (
        <span className="font-mono text-xs text-slate-200" title={row.path}>
          {row.path.length > 38 ? `${row.path.substring(0, 38)}...` : row.path}
        </span>
      ),
    },
    {
      header: "Source",
      accessor: (row) => (
        <span className="font-mono text-xs text-slate-400">{row.sourceIp}</span>
      ),
    },
    {
      header: "Status",
      accessor: (row) => {
        const code = row.responseStatus;
        if (!code) return <span className="font-mono text-xs text-slate-500">-</span>;
        const color =
          code >= 500
            ? "text-rose-400"
            : code === 403
            ? "text-rose-400 font-bold"
            : code === 429
            ? "text-purple-400 font-bold"
            : code >= 400
            ? "text-amber-400"
            : "text-emerald-400";
        return <span className={`font-mono text-xs font-bold ${color}`}>{code}</span>;
      },
    },
    {
      header: "Threat Score",
      accessor: (row) => (
        <span
          className={`font-mono text-xs font-bold ${
            row.threatScore >= 80
              ? "text-rose-400"
              : row.threatScore >= 60
              ? "text-amber-400"
              : row.threatScore >= 30
              ? "text-yellow-400"
              : "text-cyan-400"
          }`}
        >
          {row.threatScore.toFixed(0)}
        </span>
      ),
    },
    {
      header: "Decision",
      accessor: (row) => <ActionBadge action={row.verdict} />,
    },
    {
      header: "Timestamp",
      accessor: (row) => (
        <span
          className="text-xs text-slate-400 whitespace-nowrap"
          title={formatDate(row.timestamp)}
        >
          {formatRelativeTime(row.timestamp)}
        </span>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header & Filter Toolbar */}
      <div className="rounded-xl border border-slate-800 bg-sentinel-900/60 p-5 backdrop-blur-md">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-xl font-bold text-slate-100">Live API Request Stream</h1>
            <p className="text-xs text-slate-400">
              Real-time telemetry of intercepted HTTP traffic, latency, evaluation scores, and verdicts
            </p>
          </div>
          <button
            onClick={() => loadRequests()}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-800 bg-sentinel-950 px-3 py-2 text-xs font-medium text-slate-300 hover:bg-slate-800"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
            Refresh Requests
          </button>
        </div>

        {/* Filter Controls */}
        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
            <input
              type="text"
              placeholder="Filter by endpoint path..."
              value={searchPath}
              onChange={(e) => {
                setSearchPath(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 pl-9 pr-3 text-xs text-slate-200 placeholder-slate-500 focus:border-cyan-500 focus:outline-none"
            />
          </div>

          <div>
            <select
              value={httpMethod}
              onChange={(e) => {
                setHttpMethod(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 px-3 text-xs text-slate-300 focus:border-cyan-500 focus:outline-none"
            >
              <option value="">All HTTP Methods</option>
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
              <option value="PATCH">PATCH</option>
            </select>
          </div>

          <div>
            <input
              type="text"
              placeholder="Filter by IP address..."
              value={sourceIp}
              onChange={(e) => {
                setSourceIp(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 px-3 text-xs text-slate-200 placeholder-slate-500 focus:border-cyan-500 focus:outline-none"
            />
          </div>

          <div>
            <select
              value={verdict}
              onChange={(e) => {
                setVerdict(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 px-3 text-xs text-slate-300 focus:border-cyan-500 focus:outline-none"
            >
              <option value="">All Decisions</option>
              <option value="ALLOWED">ALLOWED</option>
              <option value="MONITORED">MONITORED</option>
              <option value="WARNED">WARNED</option>
              <option value="RATE_LIMITED">RATE_LIMITED</option>
              <option value="BLOCKED">BLOCKED</option>
            </select>
          </div>
        </div>
      </div>

      {/* Requests Table */}
      <DataTable
        columns={columns}
        data={requests}
        loading={loading}
        emptyMessage="No intercepted requests match the current filters."
        onRowClick={handleRowClick}
        pagination={{
          currentPage: page,
          totalPages,
          totalElements,
          onPageChange: (newPage) => setPage(newPage),
        }}
      />

      {/* Request Inspection Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Intercepted Request Telemetry"
        maxWidth="xl"
      >
        {selectedRequest && (
          <div className="space-y-4 text-xs">
            <div className="flex items-center justify-between rounded-lg border border-slate-800 bg-sentinel-950/80 p-3">
              <div className="flex items-center gap-2">
                <MethodBadge method={selectedRequest.httpMethod} />
                <span className="font-mono text-sm font-bold text-cyan-300">
                  {selectedRequest.path}
                </span>
              </div>
              <ActionBadge action={selectedRequest.verdict} />
            </div>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 rounded-lg border border-slate-800 bg-sentinel-950/80 p-3">
              <div>
                <span className="text-slate-500">Source IP</span>
                <div className="font-mono font-bold text-slate-200">{selectedRequest.sourceIp}</div>
              </div>
              <div>
                <span className="text-slate-500">Threat Score</span>
                <div className="font-mono font-bold text-cyan-400">
                  {selectedRequest.threatScore.toFixed(0)} / 100
                </div>
              </div>
              <div>
                <span className="text-slate-500">Latency</span>
                <div className="font-mono font-bold text-slate-200">
                  {selectedRequest.latencyMs} ms
                </div>
              </div>
              <div>
                <span className="text-slate-500">Auth Status</span>
                <div className="font-mono text-slate-300">{selectedRequest.authStatus}</div>
              </div>
            </div>

            <div className="space-y-2">
              <div>
                <span className="text-slate-400 font-semibold">User Agent / Client Identity</span>
                <div className="mt-1 rounded border border-slate-800 bg-slate-900 p-2.5 font-mono text-slate-300">
                  {selectedRequest.userAgent || "Unknown User Agent"}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <span className="text-slate-400 font-semibold">Request Size</span>
                  <div className="mt-1 rounded border border-slate-800 bg-slate-900 p-2.5 font-mono text-slate-300">
                    {selectedRequest.requestSizeBytes} bytes
                  </div>
                </div>
                <div>
                  <span className="text-slate-400 font-semibold">Timestamp</span>
                  <div className="mt-1 rounded border border-slate-800 bg-slate-900 p-2.5 text-slate-300">
                    {formatDate(selectedRequest.timestamp)}
                  </div>
                </div>
              </div>

              {selectedRequest.verdictReason && (
                <div>
                  <span className="text-slate-400 font-semibold">Policy Decision Reason</span>
                  <div className="mt-1 rounded border border-slate-800 bg-slate-900 p-2.5 text-slate-200">
                    {selectedRequest.verdictReason}
                  </div>
                </div>
              )}
            </div>

            <div className="flex justify-end pt-3 border-t border-slate-800">
              <button
                onClick={() => setModalOpen(false)}
                className="rounded-lg bg-slate-800 px-4 py-2 font-medium text-slate-200 hover:bg-slate-700"
              >
                Close Inspector
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
