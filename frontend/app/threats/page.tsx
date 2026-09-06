"use client";

import React, { useEffect, useState, useCallback } from "react";
import {
  ShieldAlert,
  Search,
  Filter,
  RefreshCw,
  ExternalLink,
  Code,
} from "lucide-react";
import { DataTable, Column } from "@/components/ui/DataTable";
import { SeverityBadge, ActionBadge } from "@/components/ui/Badges";
import { Modal } from "@/components/ui/Modal";
import { fetchSecurityEvents, fetchSecurityEventById } from "@/lib/api";
import { SecurityEventDto, SecurityEventDetailDto } from "@/types";
import { formatDate, formatRelativeTime } from "@/lib/utils";

export default function ThreatsPage() {
  const [events, setEvents] = useState<SecurityEventDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [search, setSearch] = useState("");
  const [severity, setSeverity] = useState("");
  const [threatType, setThreatType] = useState("");
  const [actionTaken, setActionTaken] = useState("");

  // Modal inspection
  const [selectedEvent, setSelectedEvent] = useState<SecurityEventDetailDto | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const loadEvents = useCallback(async () => {
    setLoading(true);
    const res = await fetchSecurityEvents({
      page,
      size: 15,
      search: search || undefined,
      severity: severity || undefined,
      threatType: threatType || undefined,
      actionTaken: actionTaken || undefined,
    });
    setEvents(res.content);
    setTotalPages(res.totalPages || 1);
    setTotalElements(res.totalElements || 0);
    setLoading(false);
  }, [page, search, severity, threatType, actionTaken]);

  useEffect(() => {
    loadEvents();
    const onOnline = () => loadEvents();
    const onUrlChanged = () => loadEvents();
    window.addEventListener("sentinel:backend-online", onOnline);
    window.addEventListener("sentinel:api-url-changed", onUrlChanged);
    return () => {
      window.removeEventListener("sentinel:backend-online", onOnline);
      window.removeEventListener("sentinel:api-url-changed", onUrlChanged);
    };
  }, [loadEvents]);

  const handleRowClick = async (event: SecurityEventDto) => {
    const detail = await fetchSecurityEventById(event.id);
    setSelectedEvent(
      detail || {
        ...event,
        detections: [],
      }
    );
    setModalOpen(true);
  };

  const columns: Column<SecurityEventDto>[] = [
    {
      header: "Threat Type",
      accessor: (row) => (
        <div className="flex items-center gap-2">
          <ShieldAlert className="h-4 w-4 text-cyan-400" />
          <span className="font-mono font-bold text-slate-100">{row.threatType}</span>
        </div>
      ),
    },
    {
      header: "Severity",
      accessor: (row) => <SeverityBadge severity={row.severity} />,
    },
    {
      header: "Endpoint",
      accessor: (row) => (
        <span className="font-mono text-xs text-slate-300 bg-slate-800/60 px-2 py-1 rounded">
          {row.endpoint}
        </span>
      ),
    },
    {
      header: "Reason",
      accessor: (row) => (
        <div className="max-w-xs truncate text-xs text-slate-400" title={row.reason}>
          {row.reason}
        </div>
      ),
    },
    {
      header: "Score",
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
      header: "Action",
      accessor: (row) => <ActionBadge action={row.actionTaken} />,
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
      {/* Header & Filter Controls */}
      <div className="rounded-xl border border-slate-800 bg-sentinel-900/60 p-5 backdrop-blur-md">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-xl font-bold text-slate-100">Security Threat Events</h1>
            <p className="text-xs text-slate-400">
              Audit log of all detected security violations and automated policy enforcement actions
            </p>
          </div>
          <button
            onClick={() => loadEvents()}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-800 bg-sentinel-950 px-3 py-2 text-xs font-medium text-slate-300 hover:bg-slate-800"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
            Refresh Feed
          </button>
        </div>

        {/* Filter Inputs */}
        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {/* Search bar */}
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
            <input
              type="text"
              placeholder="Search reason, endpoint, source..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 pl-9 pr-3 text-xs text-slate-200 placeholder-slate-500 focus:border-cyan-500 focus:outline-none"
            />
          </div>

          {/* Severity filter */}
          <div className="relative">
            <select
              value={severity}
              onChange={(e) => {
                setSeverity(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 px-3 text-xs text-slate-300 focus:border-cyan-500 focus:outline-none"
            >
              <option value="">All Severities</option>
              <option value="CRITICAL">CRITICAL</option>
              <option value="HIGH">HIGH</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="LOW">LOW</option>
            </select>
          </div>

          {/* Threat type filter */}
          <div className="relative">
            <select
              value={threatType}
              onChange={(e) => {
                setThreatType(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 px-3 text-xs text-slate-300 focus:border-cyan-500 focus:outline-none"
            >
              <option value="">All Threat Types</option>
              <option value="SQL_INJECTION">SQL Injection</option>
              <option value="AUTH_ABUSE">Authentication Abuse</option>
              <option value="RATE_ABUSE">Rate Abuse</option>
              <option value="ENUMERATION">Enumeration</option>
              <option value="SENSITIVE_DATA_EXPOSURE">Sensitive Data Exposure</option>
            </select>
          </div>

          {/* Action filter */}
          <div className="relative">
            <select
              value={actionTaken}
              onChange={(e) => {
                setActionTaken(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 py-2 px-3 text-xs text-slate-300 focus:border-cyan-500 focus:outline-none"
            >
              <option value="">All Actions</option>
              <option value="BLOCK">BLOCK</option>
              <option value="RATE_LIMIT">RATE_LIMIT</option>
              <option value="WARN">WARN</option>
              <option value="MONITOR">MONITOR</option>
              <option value="ALLOW">ALLOW</option>
            </select>
          </div>
        </div>
      </div>

      {/* Events Table */}
      <DataTable
        columns={columns}
        data={events}
        loading={loading}
        emptyMessage="No threat events found matching current filter criteria."
        onRowClick={handleRowClick}
        pagination={{
          currentPage: page,
          totalPages,
          totalElements,
          onPageChange: (newPage) => setPage(newPage),
        }}
      />

      {/* Incident Detail Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title="Security Incident Deep Inspection"
        maxWidth="xl"
      >
        {selectedEvent && (
          <div className="space-y-4 text-xs">
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 rounded-lg border border-slate-800 bg-sentinel-950/80 p-3">
              <div>
                <span className="text-slate-500">Threat Type</span>
                <div className="font-mono font-bold text-slate-200">{selectedEvent.threatType}</div>
              </div>
              <div>
                <span className="text-slate-500">Severity</span>
                <div className="mt-0.5">
                  <SeverityBadge severity={selectedEvent.severity} />
                </div>
              </div>
              <div>
                <span className="text-slate-500">Risk Score</span>
                <div className="font-mono font-bold text-cyan-400">
                  {selectedEvent.threatScore.toFixed(1)} / 100
                </div>
              </div>
              <div>
                <span className="text-slate-500">Action Taken</span>
                <div className="mt-0.5">
                  <ActionBadge action={selectedEvent.actionTaken} />
                </div>
              </div>
            </div>

            <div className="space-y-2">
              <div>
                <span className="text-slate-400 font-semibold">Target Endpoint</span>
                <div className="mt-1 rounded border border-slate-800 bg-slate-900 px-3 py-2 font-mono text-cyan-300">
                  {selectedEvent.endpoint}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <span className="text-slate-400 font-semibold">Source Client / IP</span>
                  <div className="mt-1 rounded border border-slate-800 bg-slate-900 px-3 py-2 font-mono text-slate-300">
                    {selectedEvent.source}
                  </div>
                </div>
                <div>
                  <span className="text-slate-400 font-semibold">Exact Timestamp</span>
                  <div className="mt-1 rounded border border-slate-800 bg-slate-900 px-3 py-2 text-slate-300">
                    {formatDate(selectedEvent.timestamp)}
                  </div>
                </div>
              </div>

              <div>
                <span className="text-slate-400 font-semibold">Detection Reason</span>
                <div className="mt-1 rounded border border-slate-800 bg-slate-900/90 p-3 text-slate-200">
                  {selectedEvent.reason}
                </div>
              </div>

              <div>
                <span className="text-slate-400 font-semibold">Captured Evidence</span>
                <div className="mt-1 overflow-x-auto rounded border border-slate-800 bg-sentinel-950 p-3 font-mono text-rose-300">
                  {selectedEvent.evidence || "No raw evidence payload available"}
                </div>
              </div>

              {selectedEvent.detections && selectedEvent.detections.length > 0 && (
                <div>
                  <span className="text-slate-400 font-semibold">Detector Breakdown</span>
                  <div className="mt-1 space-y-2">
                    {selectedEvent.detections.map((det, idx) => (
                      <div
                        key={idx}
                        className="rounded border border-slate-800 bg-slate-900 p-2.5 flex items-center justify-between"
                      >
                        <div className="space-y-0.5">
                          <span className="font-semibold text-slate-200">{det.threatType}</span>
                          <p className="text-[11px] text-slate-400">{det.reason}</p>
                        </div>
                        <span className="font-mono font-bold text-amber-400">
                          +{det.scoreContribution} pts
                        </span>
                      </div>
                    ))}
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
