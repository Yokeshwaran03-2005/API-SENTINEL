import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import { SeverityLevel, PolicyAction } from "@/types";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(dateString?: string): string {
  if (!dateString) return "N/A";
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    return new Intl.DateTimeFormat("en-US", {
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    }).format(date);
  } catch {
    return dateString;
  }
}

export function formatRelativeTime(dateString?: string): string {
  if (!dateString) return "just now";
  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffSeconds < 5) return "just now";
    if (diffSeconds < 60) return `${diffSeconds}s ago`;
    const diffMinutes = Math.floor(diffSeconds / 60);
    if (diffMinutes < 60) return `${diffMinutes}m ago`;
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays}d ago`;
  } catch {
    return dateString;
  }
}

export function getSeverityBadgeClass(severity: SeverityLevel | string): string {
  switch (severity?.toUpperCase()) {
    case "CRITICAL":
      return "bg-rose-500/15 text-rose-400 border-rose-500/30";
    case "HIGH":
      return "bg-amber-500/15 text-amber-400 border-amber-500/30";
    case "MEDIUM":
      return "bg-yellow-500/15 text-yellow-400 border-yellow-500/30";
    case "LOW":
    default:
      return "bg-cyan-500/15 text-cyan-400 border-cyan-500/30";
  }
}

export function getActionBadgeClass(action: PolicyAction | string): string {
  switch (action?.toUpperCase()) {
    case "BLOCK":
    case "BLOCKED":
      return "bg-rose-600/20 text-rose-300 border-rose-500/40";
    case "RATE_LIMIT":
    case "RATE_LIMITED":
      return "bg-purple-600/20 text-purple-300 border-purple-500/40";
    case "WARN":
    case "WARNED":
      return "bg-amber-500/20 text-amber-300 border-amber-500/40";
    case "MONITOR":
    case "MONITORED":
      return "bg-blue-500/20 text-blue-300 border-blue-500/40";
    case "ALLOW":
    case "ALLOWED":
    default:
      return "bg-emerald-500/20 text-emerald-300 border-emerald-500/40";
  }
}

export function getMethodBadgeClass(method: string): string {
  switch (method?.toUpperCase()) {
    case "GET":
      return "bg-blue-500/15 text-blue-400 border-blue-500/30";
    case "POST":
      return "bg-emerald-500/15 text-emerald-400 border-emerald-500/30";
    case "PUT":
      return "bg-amber-500/15 text-amber-400 border-amber-500/30";
    case "DELETE":
      return "bg-rose-500/15 text-rose-400 border-rose-500/30";
    case "PATCH":
      return "bg-purple-500/15 text-purple-400 border-purple-500/30";
    default:
      return "bg-slate-500/15 text-slate-400 border-slate-500/30";
  }
}

export function getRiskLevelFromScore(score: number): SeverityLevel {
  if (score >= 80) return "CRITICAL";
  if (score >= 60) return "HIGH";
  if (score >= 30) return "MEDIUM";
  return "LOW";
}
