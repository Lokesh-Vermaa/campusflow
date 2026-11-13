import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, renderHook, act } from '@testing-library/react';
import { ThemeToggle } from './ThemeToggle';
import { useTheme, THEME_STORAGE_KEY, applyTheme } from '@/hooks/useTheme';

describe('useTheme hook & applyTheme', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.className = '';
    document.documentElement.removeAttribute('data-theme');
    vi.clearAllMocks();
  });

  it('applies dark theme correctly to DOM root', () => {
    applyTheme('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('applies light theme correctly to DOM root', () => {
    applyTheme('dark');
    applyTheme('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('useTheme hook toggles and stores preference in localStorage', () => {
    const { result } = renderHook(() => useTheme());

    // Initially light (or system)
    act(() => {
      result.current.setTheme('dark');
    });

    expect(result.current.theme).toBe('dark');
    expect(result.current.isDark).toBe(true);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    // Toggle back to light
    act(() => {
      result.current.toggleTheme();
    });

    expect(result.current.theme).toBe('light');
    expect(result.current.isDark).toBe(false);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});

describe('ThemeToggle Component', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.className = '';
    document.documentElement.removeAttribute('data-theme');
  });

  it('renders the theme toggle button with accessible label and title', () => {
    render(<ThemeToggle />);
    const button = screen.getByRole('button', { name: /toggle theme/i });
    expect(button).toBeInTheDocument();
  });

  it('opens menu with keyboard interaction (Enter key)', () => {
    render(<ThemeToggle />);
    const trigger = screen.getByRole('button', { name: /toggle theme/i });
    fireEvent.keyDown(trigger, { key: 'Enter', code: 'Enter' });

    // Verify menu items appear
    expect(screen.getByRole('menuitem', { name: /light/i })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /dark/i })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /system/i })).toBeInTheDocument();
  });

  it('selects dark theme via menuitem click', () => {
    render(<ThemeToggle />);
    const trigger = screen.getByRole('button', { name: /toggle theme/i });
    fireEvent.keyDown(trigger, { key: 'Enter', code: 'Enter' });

    const darkItem = screen.getByRole('menuitem', { name: /dark/i });
    fireEvent.click(darkItem);

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
  });
});
