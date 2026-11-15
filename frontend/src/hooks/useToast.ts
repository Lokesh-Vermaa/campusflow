import { useState, useCallback, useEffect } from 'react';

type ToastVariant = 'default' | 'destructive' | 'success';

export interface ToastMessage {
  id: string;
  title?: string;
  description?: string;
  variant?: ToastVariant;
  duration?: number;
}

let toastCount = 0;

// Global toast state (simple singleton)
let listeners: Array<(toasts: ToastMessage[]) => void> = [];
let toasts: ToastMessage[] = [];

function dispatch(toast: ToastMessage) {
  toasts = [...toasts, toast];
  listeners.forEach((listener) => listener(toasts));

  setTimeout(() => {
    toasts = toasts.filter((t) => t.id !== toast.id);
    listeners.forEach((listener) => listener(toasts));
  }, toast.duration ?? 5000);
}

export function useToast() {
  const [state, setState] = useState<ToastMessage[]>(toasts);

  useEffect(() => {
    listeners.push(setState);
    return () => {
      listeners = listeners.filter((l) => l !== setState);
    };
  }, []);

  const toast = useCallback(
    (message: Omit<ToastMessage, 'id'>) => {
      const id = String(++toastCount);
      dispatch({ ...message, id });
    },
    [],
  );

  return { toasts: state, toast };
}
