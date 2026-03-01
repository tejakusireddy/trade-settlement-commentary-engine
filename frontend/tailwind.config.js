/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        'bg-base': '#0A0B0D',
        'bg-surface': '#111318',
        'bg-raised': '#1A1D24',
        'border-subtle': '#2A2D35',
        primary: '#00D4AA',
        danger: '#FF4D4D',
        warning: '#FFB547',
        info: '#4D9EFF',
        'ai-accent': '#7C6EF8',
        'text-primary': '#F0F2F5',
        'text-secondary': '#8B92A5',
        'text-tertiary': '#4A5060',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      boxShadow: {
        card: '0 1px 0 rgba(255,255,255,0.03), 0 10px 30px rgba(0,0,0,0.45)',
        panel: 'inset 0 1px 0 rgba(255,255,255,0.03), 0 4px 16px rgba(0,0,0,0.35)',
        glow: '0 0 0 1px rgba(0,212,170,0.12), 0 0 24px rgba(0,212,170,0.08)',
      },
    },
  },
  plugins: [],
}

