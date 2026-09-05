import type { Metadata } from "next";
import "./globals.css";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";

export const metadata: Metadata = {
  title: "API Sentinel | Runtime Security & Threat Detection",
  description:
    "Enterprise API Security & Threat Detection Platform with real-time scoring, rate limiting, and automated policy enforcement.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark">
      <body className="bg-sentinel-950 text-slate-100 antialiased selection:bg-cyan-500/30 selection:text-cyan-200">
        <div className="flex min-h-screen">
          {/* Fixed Left Sidebar */}
          <Sidebar />

          {/* Main App Container */}
          <div className="flex flex-1 flex-col pl-64">
            <Header />
            <main className="flex-1 p-6 md:p-8">
              <div className="mx-auto max-w-7xl">{children}</div>
            </main>
          </div>
        </div>
      </body>
    </html>
  );
}
