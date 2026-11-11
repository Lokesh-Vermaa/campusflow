import { cn } from '@/lib/cn';

interface LogoProps {
  variant?: 'light' | 'color';
  className?: string;
}

export function Logo({ variant = 'color', className }: LogoProps) {
  const isLight = variant === 'light';

  return (
    <span
      className={cn('inline-flex items-baseline font-heading text-2xl font-bold leading-none', className)}
      aria-label="JKLU CampusFlow"
    >
      <span className={isLight ? 'text-white' : 'text-brand-azure-strong'}>JKLU</span>
      <span
        className="ml-0.5 text-brand-orange"
        aria-hidden="true"
      >
        .
      </span>
      <span className={cn('ml-1 text-sm font-semibold tracking-wide', isLight ? 'text-white/80' : 'text-text-heading')}>
        CampusFlow
      </span>
    </span>
  );
}
