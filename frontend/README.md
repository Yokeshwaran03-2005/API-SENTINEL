# API SENTINEL - Frontend Client

Modern, responsive Next.js TypeScript web application for the **API SENTINEL** Real-Time API Security & Threat Control Platform.

## Target Deployment
* **Platform**: [Vercel](https://vercel.com)
* **Framework**: Next.js 14+ (App Router)
* **Language**: TypeScript
* **State & Data**: React hooks, WebSocket real-time subscription client

## Directory Structure

```
frontend/
├── app/                  # Next.js App Router root
│   ├── dashboard/        # Main Security Operations Center (SOC) dashboard
│   ├── threats/          # Real-time threat feed and incident management
│   ├── endpoints/        # Monitored API inventory and endpoint configuration
│   ├── requests/         # API traffic inspector and detailed request logs
│   ├── policies/         # WAF/Rate limiting rules and automated policy engine
│   └── simulator/        # Attack traffic generator and simulation studio
├── components/           # Reusable UI components (charts, tables, badges, modals)
├── lib/                  # Utility functions, API clients, WebSocket clients
├── public/               # Static assets, SVG icons, logos
└── types/                # TypeScript interface and type declarations
```

## Getting Started

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) to view the application in the browser.
