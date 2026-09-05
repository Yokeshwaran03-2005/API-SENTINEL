import React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

export interface Column<T> {
  header: string;
  accessor?: keyof T | ((row: T) => React.ReactNode);
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  loading?: boolean;
  emptyMessage?: string;
  onRowClick?: (row: T) => void;
  pagination?: {
    currentPage: number;
    totalPages: number;
    totalElements: number;
    onPageChange: (newPage: number) => void;
  };
}

export function DataTable<T extends { id?: number | string }>({
  columns,
  data,
  loading,
  emptyMessage = "No records found.",
  onRowClick,
  pagination,
}: DataTableProps<T>) {
  return (
    <div className="overflow-hidden rounded-xl border border-slate-800 bg-sentinel-900/60 backdrop-blur-md">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-slate-300">
          <thead className="border-b border-slate-800 bg-sentinel-950/70 text-xs uppercase tracking-wider text-slate-400">
            <tr>
              {columns.map((col, idx) => (
                <th
                  key={idx}
                  className={`px-4 py-3.5 font-semibold ${col.className || ""}`}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {loading ? (
              Array.from({ length: 5 }).map((_, rIdx) => (
                <tr key={rIdx} className="animate-pulse">
                  {columns.map((_, cIdx) => (
                    <td key={cIdx} className="px-4 py-4">
                      <div className="h-4 rounded bg-slate-800" />
                    </td>
                  ))}
                </tr>
              ))
            ) : data.length === 0 ? (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-12 text-center text-sm text-slate-500"
                >
                  {emptyMessage}
                </td>
              </tr>
            ) : (
              data.map((row, rIdx) => (
                <tr
                  key={row.id ? String(row.id) : rIdx}
                  onClick={() => onRowClick && onRowClick(row)}
                  className={`transition-colors hover:bg-slate-800/40 ${
                    onRowClick ? "cursor-pointer" : ""
                  }`}
                >
                  {columns.map((col, cIdx) => (
                    <td
                      key={cIdx}
                      className={`px-4 py-3.5 align-middle ${col.className || ""}`}
                    >
                      {typeof col.accessor === "function"
                        ? col.accessor(row)
                        : col.accessor
                        ? (row[col.accessor] as React.ReactNode)
                        : null}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {pagination && (
        <div className="flex items-center justify-between border-t border-slate-800 px-4 py-3 text-xs text-slate-400">
          <div>
            Showing page <span className="font-semibold text-slate-200">{pagination.currentPage + 1}</span> of{" "}
            <span className="font-semibold text-slate-200">{Math.max(1, pagination.totalPages)}</span> ({pagination.totalElements} total)
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => pagination.onPageChange(pagination.currentPage - 1)}
              disabled={pagination.currentPage <= 0}
              className="inline-flex items-center gap-1 rounded-md border border-slate-800 bg-sentinel-950 px-2.5 py-1.5 font-medium hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="h-3.5 w-3.5" />
              Previous
            </button>
            <button
              onClick={() => pagination.onPageChange(pagination.currentPage + 1)}
              disabled={pagination.currentPage >= pagination.totalPages - 1}
              className="inline-flex items-center gap-1 rounded-md border border-slate-800 bg-sentinel-950 px-2.5 py-1.5 font-medium hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Next
              <ChevronRight className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
