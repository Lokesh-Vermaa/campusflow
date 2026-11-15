import { Moon, Sun, Laptop, Check } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
} from '@/components/ui/DropdownMenu';
import { cn } from '@/lib/cn';

interface ThemeToggleProps {
  className?: string;
  align?: 'start' | 'center' | 'end';
}

export function ThemeToggle({ className, align = 'end' }: ThemeToggleProps) {
  const { theme, setTheme, isDark } = useTheme();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          aria-label="Toggle theme"
          title={`Current theme: ${theme}. Click to change theme.`}
          className={cn(
            'inline-flex h-9 w-9 items-center justify-center rounded-lg border border-border-color bg-surface text-text-heading shadow-2xs transition hover:bg-surface-alt focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-azure-strong',
            className,
          )}
        >
          {isDark ? (
            <Sun className="h-4 w-4 text-amber-400 transition-transform hover:rotate-45" aria-hidden="true" />
          ) : (
            <Moon className="h-4 w-4 text-text-heading transition-transform hover:-rotate-12" aria-hidden="true" />
          )}
          <span className="sr-only">Toggle theme</span>
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align={align} className="w-36">
        <DropdownMenuLabel className="text-[11px] font-semibold text-text-body/60 uppercase tracking-wider">
          Theme
        </DropdownMenuLabel>
        <DropdownMenuSeparator />

        <DropdownMenuItem
          onClick={() => setTheme('light')}
          className="flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-2">
            <Sun className="h-3.5 w-3.5 text-amber-500" aria-hidden="true" />
            <span>Light</span>
          </div>
          {theme === 'light' && <Check className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />}
        </DropdownMenuItem>

        <DropdownMenuItem
          onClick={() => setTheme('dark')}
          className="flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-2">
            <Moon className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />
            <span>Dark</span>
          </div>
          {theme === 'dark' && <Check className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />}
        </DropdownMenuItem>

        <DropdownMenuItem
          onClick={() => setTheme('system')}
          className="flex items-center justify-between cursor-pointer"
        >
          <div className="flex items-center gap-2">
            <Laptop className="h-3.5 w-3.5 text-text-body/70" aria-hidden="true" />
            <span>System</span>
          </div>
          {theme === 'system' && <Check className="h-3.5 w-3.5 text-brand-azure-strong" aria-hidden="true" />}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
