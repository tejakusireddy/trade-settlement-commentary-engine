/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        'bg-base': '#080A0F',
        'bg-surface': '#0D1117',
        'bg-raised': '#161B22',
        'bg-high': '#1C2128',
        'border-subtle': '#21262D',
        'border-focus': '#388BFD',
        primary: '#388BFD',
        danger: '#F85149',
        warning: '#D29922',
        info: '#388BFD',
        success: '#3FB950',
        'ai-accent': '#BC8CFF',
        'text-primary': '#E6EDF3',
        'text-secondary': '#7D8590',
        'text-tertiary': '#484F58',
        'text-link': '#388BFD',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['IBM Plex Mono', 'SFMono-Regular', 'Menlo', 'Monaco', 'monospace'],
      },
    },
  },
  plugins: [],
}

