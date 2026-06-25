/** @type {import('tailwindcss').Config} */

export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    container: {
      center: true,
    },
    extend: {
      colors: {
        // 浅色基底 - 蓝白系
        bark: "#F0F7FF",
        moss: "#E0EFFF",
        mossDeep: "#FFFFFF",
        mossLight: "#DBEAFE",
        // 强调色 - 蓝色系
        leaf: "#3B82F6",
        leafBright: "#60A5FA",
        water: "#0EA5E9",
        waterDeep: "#0284C7",
        // 文字与中性
        cream: "#1E3A5F",
        creamDim: "#64748B",
        ash: "#94A3B8",
        // 状态色
        amber: "#F59E0B",
        rust: "#EF4444",
      },
      fontFamily: {
        display: ['"Fraunces"', 'serif'],
        body: ['"Manrope"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      backgroundImage: {
        'leaf-vein': "radial-gradient(circle at 20% 30%, rgba(59,130,246,0.06) 0%, transparent 50%), radial-gradient(circle at 80% 70%, rgba(14,165,233,0.05) 0%, transparent 50%)",
        'moss-gradient': "linear-gradient(135deg, #F0F7FF 0%, #FFFFFF 100%)",
      },
      boxShadow: {
        glass: "0 4px 24px 0 rgba(59,130,246,0.08), inset 0 1px 0 0 rgba(255,255,255,0.8)",
        'leaf-glow': "0 0 20px rgba(59,130,246,0.15)",
        'water-glow': "0 0 20px rgba(14,165,233,0.15)",
      },
      animation: {
        'breathe': 'breathe 3s ease-in-out infinite',
        'pulse-ring': 'pulse-ring 2s cubic-bezier(0.4,0,0.6,1) infinite',
        'fade-in': 'fade-in 0.4s ease-out',
        'slide-up': 'slide-up 0.5s ease-out',
      },
      keyframes: {
        breathe: {
          '0%, 100%': { opacity: '1', transform: 'scale(1)' },
          '50%': { opacity: '0.7', transform: 'scale(0.97)' },
        },
        'pulse-ring': {
          '0%': { transform: 'scale(0.8)', opacity: '0.8' },
          '100%': { transform: 'scale(2)', opacity: '0' },
        },
        'fade-in': {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        'slide-up': {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
};
