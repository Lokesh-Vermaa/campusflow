import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './styles/tokens.css';
import App from './App';
import { enableMocking } from './mocks/browser';
import { applyTheme } from './hooks/useTheme';

// Initialize theme on initial load before mount
try {
  const stored = localStorage.getItem('jklu-campusflow-theme');
  if (stored === 'dark' || stored === 'light' || stored === 'system') {
    applyTheme(stored);
  } else {
    applyTheme('system');
  }
} catch {
  // fallback if storage access fails
}

async function prepare() {
  if (import.meta.env.VITE_USE_MOCKS !== 'false') {
    await enableMocking();
  }
}

prepare().then(() => {
  const rootElement = document.getElementById('root');
  if (!rootElement) {
    throw new Error('Root element not found');
  }

  createRoot(rootElement).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
});
