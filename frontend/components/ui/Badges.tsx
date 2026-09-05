import React from "react";
import {
  getSeverityBadgeClass,
  getActionBadgeClass,
  getMethodBadgeClass,
} from "@/lib/utils";
import { SeverityLevel, PolicyAction } from "@/types";

export function SeverityBadge({
  severity,
  className = "",
}: {
  severity: SeverityLevel | string;
  className?: string;
}) {
  const badgeClass = getSeverityBadgeClass(severity);
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold uppercase tracking-wider border ${badgeClass} ${className}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current mr-1.5 opacity-80" />
      {severity}
    </span>
  );
}

export function ActionBadge({
  action,
  className = "",
}: {
  action: PolicyAction | string;
  className?: string;
}) {
  const badgeClass = getActionBadgeClass(action);
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-semibold uppercase tracking-wider border ${badgeClass} ${className}`}
    >
      {action}
    </span>
  );
}

export function MethodBadge({
  method,
  className = "",
}: {
  method: string;
  className?: string;
}) {
  const badgeClass = getMethodBadgeClass(method);
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded font-mono text-xs font-bold border ${badgeClass} ${className}`}
    >
      {method}
    </span>
  );
}
