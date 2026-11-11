import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@/styles/tokens.css';
import App from '@/App';
import { enableMocking } from '@/mocks/browser';

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
