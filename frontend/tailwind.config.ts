import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./lib/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        sentinel: {
          950: "#060913",
          900: "#0c1222",
          850: "#111a33",
          800: "#172344",
          700: "#1f305c",
          600: "#2d4480",
          500: "#3d5da8",
        },
        cyber: {
          cyan: "#06b6d4",
          blue: "#3b82f6",
          emerald: "#10b981",
          amber: "#f59e0b",
          rose: "#f43f5e",
          purple: "#a855f7",
        },
      },
      backgroundImage: {
        "gradient-radial": "radial-gradient(var(--tw-gradient-stops))",
        "cyber-grid": "linear-gradient(to right, rgba(255, 255, 255, 0.04) 1px, transparent 1px), linear-gradient(to bottom, rgba(255, 255, 255, 0.04) 1px, transparent 1px)",
      },
      animation: {
        "pulse-slow": "pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite",
        "glow": "glow 2s ease-in-out infinite alternate",
      },
      keyframes: {
        glow: {
          "0%": { boxShadow: "0 0 10px rgba(6, 182, 212, 0.2)" },
          "100%": { boxShadow: "0 0 25px rgba(6, 182, 212, 0.6)" },
        },
      },
    },
  },
  plugins: [],
};

export default config;
