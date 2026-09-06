"use client";

import React, { useEffect, useState } from "react";
import { Sliders, RefreshCw, CheckCircle2, AlertCircle, Edit3, Shield } from "lucide-react";
import { ActionBadge } from "@/components/ui/Badges";
import { Modal } from "@/components/ui/Modal";
import { fetchPolicies, updatePolicy } from "@/lib/api";
import { SecurityPolicyDto, UpdateSecurityPolicyRequest, PolicyAction } from "@/types";

export default function PoliciesPage() {
  const [policies, setPolicies] = useState<SecurityPolicyDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);

  // Edit Modal
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState<SecurityPolicyDto | null>(null);
  const [formData, setFormData] = useState<UpdateSecurityPolicyRequest>({
    name: "",
    description: "",
    pathPattern: "",
    threatScoreThreshold: 80,
    action: "BLOCK",
    rateLimitPerMinute: 60,
    active: true,
  });

  const loadPolicies = async () => {
    setLoading(true);
    const data = await fetchPolicies();
    setPolicies(data);
    setLoading(false);
  };

  useEffect(() => {
    loadPolicies();
    const onOnline = () => loadPolicies();
    const onUrlChanged = () => loadPolicies();
    window.addEventListener("sentinel:backend-online", onOnline);
    window.addEventListener("sentinel:api-url-changed", onUrlChanged);
    return () => {
      window.removeEventListener("sentinel:backend-online", onOnline);
      window.removeEventListener("sentinel:api-url-changed", onUrlChanged);
    };
  }, []);

  const handleOpenEdit = (policy: SecurityPolicyDto) => {
    setEditingPolicy(policy);
    setFormData({
      name: policy.name,
      description: policy.description || "",
      pathPattern: policy.pathPattern,
      threatScoreThreshold: policy.threatScoreThreshold,
      action: policy.action,
      rateLimitPerMinute: policy.rateLimitPerMinute || 60,
      active: policy.active,
    });
    setFeedbackMsg(null);
    setEditModalOpen(true);
  };

  const handleSavePolicy = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingPolicy) return;

    setUpdating(true);
    setFeedbackMsg(null);
    try {
      const updated = await updatePolicy(editingPolicy.id, formData);
      setPolicies((prev) =>
        prev.map((p) => (p.id === updated.id ? updated : p))
      );
      setFeedbackMsg({
        type: "success",
        text: `Policy "${updated.name}" successfully updated in backend!`,
      });
      setTimeout(() => {
        setEditModalOpen(false);
        setFeedbackMsg(null);
      }, 1200);
    } catch (err: any) {
      setFeedbackMsg({
        type: "error",
        text: err.message || "Failed to update policy.",
      });
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="rounded-xl border border-slate-800 bg-sentinel-900/60 p-5 backdrop-blur-md">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-xl font-bold text-slate-100">Security Policies & Thresholds</h1>
            <p className="text-xs text-slate-400">
              Configure perimeter threat thresholds, rate limit quotas, and automated enforcement actions
            </p>
          </div>
          <button
            onClick={() => loadPolicies()}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-800 bg-sentinel-950 px-3 py-2 text-xs font-medium text-slate-300 hover:bg-slate-800"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
            Refresh Policies
          </button>
        </div>
      </div>

      {/* Policy Cards Grid */}
      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        {policies.map((policy) => (
          <div
            key={policy.id}
            className={`relative overflow-hidden rounded-xl border p-5 backdrop-blur-md transition-all ${
              policy.active
                ? "border-slate-800 bg-sentinel-900/80 hover:border-cyan-500/30"
                : "border-slate-800/40 bg-sentinel-950/40 opacity-70"
            }`}
          >
            <div className="flex items-start justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-bold text-slate-100">{policy.name}</h3>
                  <span
                    className={`rounded px-1.5 py-0.5 text-[10px] font-bold ${
                      policy.active
                        ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                        : "bg-slate-800 text-slate-400"
                    }`}
                  >
                    {policy.active ? "ACTIVE" : "DISABLED"}
                  </span>
                </div>
                <p className="mt-1 text-xs text-slate-400">{policy.description}</p>
              </div>

              <ActionBadge action={policy.action} />
            </div>

            {/* Path pattern badge */}
            <div className="mt-4 flex items-center gap-2">
              <span className="text-xs text-slate-500">Pattern:</span>
              <code className="rounded bg-slate-800/80 px-2 py-0.5 font-mono text-xs text-cyan-300">
                {policy.pathPattern}
              </code>
            </div>

            {/* Policy Parameters */}
            <div className="mt-4 grid grid-cols-2 gap-3 rounded-lg border border-slate-800 bg-sentinel-950/80 p-3">
              <div>
                <span className="text-[11px] text-slate-500 uppercase tracking-wider font-medium">
                  Threat Threshold
                </span>
                <div className="mt-1 flex items-baseline gap-1.5">
                  <span
                    className={`font-mono text-xl font-bold ${
                      policy.threatScoreThreshold >= 80
                        ? "text-rose-400"
                        : policy.threatScoreThreshold >= 60
                        ? "text-amber-400"
                        : "text-cyan-400"
                    }`}
                  >
                    {policy.threatScoreThreshold}
                  </span>
                  <span className="text-xs text-slate-500">/ 100</span>
                </div>
                <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-slate-800">
                  <div
                    style={{ width: `${policy.threatScoreThreshold}%` }}
                    className={`h-full ${
                      policy.threatScoreThreshold >= 80
                        ? "bg-rose-500"
                        : policy.threatScoreThreshold >= 60
                        ? "bg-amber-500"
                        : "bg-cyan-500"
                    }`}
                  />
                </div>
              </div>

              <div>
                <span className="text-[11px] text-slate-500 uppercase tracking-wider font-medium">
                  Rate Limit Quota
                </span>
                <div className="mt-1 flex items-baseline gap-1.5">
                  <span className="font-mono text-xl font-bold text-slate-200">
                    {policy.rateLimitPerMinute}
                  </span>
                  <span className="text-xs text-slate-500">req / min</span>
                </div>
                <div className="mt-2 text-[10px] text-slate-500">
                  Sliding window per IP
                </div>
              </div>
            </div>

            {/* Action footer */}
            <div className="mt-4 flex items-center justify-between border-t border-slate-800/80 pt-3">
              <span className="text-[11px] text-slate-500">
                Enforcement: <span className="text-slate-300 font-medium">{policy.action}</span>
              </span>
              <button
                onClick={() => handleOpenEdit(policy)}
                className="inline-flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800/60 px-3 py-1.5 text-xs font-semibold text-slate-200 hover:border-cyan-500/40 hover:bg-slate-800"
              >
                <Edit3 className="h-3.5 w-3.5 text-cyan-400" />
                Update Policy
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Edit Policy Modal */}
      <Modal
        isOpen={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        title={editingPolicy ? `Update Policy: ${editingPolicy.name}` : "Edit Security Policy"}
        maxWidth="md"
      >
        <form onSubmit={handleSavePolicy} className="space-y-4 text-xs">
          {feedbackMsg && (
            <div
              className={`flex items-center gap-2 rounded-lg p-3 ${
                feedbackMsg.type === "success"
                  ? "border border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
                  : "border border-rose-500/30 bg-rose-500/10 text-rose-400"
              }`}
            >
              {feedbackMsg.type === "success" ? (
                <CheckCircle2 className="h-4 w-4" />
              ) : (
                <AlertCircle className="h-4 w-4" />
              )}
              <span>{feedbackMsg.text}</span>
            </div>
          )}

          <div>
            <label className="block font-semibold text-slate-300 mb-1">Policy Name</label>
            <input
              type="text"
              required
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 px-3 py-2 text-slate-200 focus:border-cyan-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-300 mb-1">Path Pattern</label>
            <input
              type="text"
              required
              value={formData.pathPattern}
              onChange={(e) => setFormData({ ...formData, pathPattern: e.target.value })}
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 px-3 py-2 font-mono text-cyan-300 focus:border-cyan-500 focus:outline-none"
              placeholder="e.g. /api/users/**"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-300 mb-1">
                Threat Score Threshold ({formData.threatScoreThreshold})
              </label>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={formData.threatScoreThreshold}
                onChange={(e) =>
                  setFormData({ ...formData, threatScoreThreshold: parseInt(e.target.value, 10) })
                }
                className="w-full accent-cyan-500"
              />
              <div className="flex justify-between text-[10px] text-slate-500 mt-1">
                <span>0 (Strict)</span>
                <span>50</span>
                <span>100 (Permissive)</span>
              </div>
            </div>

            <div>
              <label className="block font-semibold text-slate-300 mb-1">Enforcement Action</label>
              <select
                value={formData.action}
                onChange={(e) =>
                  setFormData({ ...formData, action: e.target.value as PolicyAction })
                }
                className="w-full rounded-lg border border-slate-800 bg-sentinel-950 px-3 py-2 text-slate-200 focus:border-cyan-500 focus:outline-none"
              >
                <option value="BLOCK">BLOCK (HTTP 403)</option>
                <option value="RATE_LIMIT">RATE_LIMIT (HTTP 429)</option>
                <option value="WARN">WARN</option>
                <option value="MONITOR">MONITOR</option>
                <option value="ALLOW">ALLOW</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block font-semibold text-slate-300 mb-1">
              Rate Limit (Requests per minute)
            </label>
            <input
              type="number"
              min="1"
              max="10000"
              required
              value={formData.rateLimitPerMinute}
              onChange={(e) =>
                setFormData({ ...formData, rateLimitPerMinute: parseInt(e.target.value, 10) || 60 })
              }
              className="w-full rounded-lg border border-slate-800 bg-sentinel-950 px-3 py-2 font-mono text-slate-200 focus:border-cyan-500 focus:outline-none"
            />
          </div>

          <div className="flex items-center gap-2 pt-2">
            <input
              type="checkbox"
              id="activePolicy"
              checked={formData.active}
              onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
              className="h-4 w-4 rounded border-slate-700 bg-sentinel-950 text-cyan-500 focus:ring-0"
            />
            <label htmlFor="activePolicy" className="font-semibold text-slate-300 cursor-pointer">
              Policy Active & Enforced
            </label>
          </div>

          <div className="flex justify-end gap-2 pt-4 border-t border-slate-800">
            <button
              type="button"
              onClick={() => setEditModalOpen(false)}
              className="rounded-lg bg-slate-800 px-4 py-2 font-medium text-slate-300 hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={updating}
              className="rounded-lg bg-cyan-600 px-4 py-2 font-medium text-white hover:bg-cyan-500 disabled:opacity-50 shadow-md shadow-cyan-600/20"
            >
              {updating ? "Saving Changes..." : "Save Policy"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
