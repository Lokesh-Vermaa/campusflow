/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        'brand-periwinkle': 'var(--brand-periwinkle)',
        'brand-periwinkle-deep': 'var(--brand-periwinkle-deep)',
        'brand-azure': 'var(--brand-azure)',
        'brand-azure-strong': 'var(--brand-azure-strong)',
        'brand-orange': 'var(--brand-orange)',
        'brand-orange-ink': 'var(--brand-orange-ink)',
        'text-heading': 'var(--text-heading)',
        'text-body': 'var(--text-body)',
        'surface': 'var(--surface)',
        'surface-alt': 'var(--surface-alt)',
        'border-color': 'var(--border)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        heading: ['Poppins', 'system-ui', 'sans-serif'],
      },
      letterSpacing: {
        'nav': '0.12em',
      },
    },
  },
  plugins: [],
};
