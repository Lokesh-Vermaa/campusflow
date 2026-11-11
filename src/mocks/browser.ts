import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

export const worker = setupWorker(...handlers);

export async function enableMocking() {
  // Only start MSW if VITE_USE_MOCKS is not explicitly false
  if (import.meta.env.VITE_USE_MOCKS === 'false') {
    return;
  }

  return worker.start({
    onUnhandledRequest: 'bypass',
    serviceWorker: {
      url: '/mockServiceWorker.js',
    },
  });
}
