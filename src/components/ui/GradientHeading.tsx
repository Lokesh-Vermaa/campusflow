import { type ReactNode } from 'react';
import { cn } from '@/lib/cn';

interface GradientHeadingProps {
  as?: 'h1' | 'h2' | 'h3';
  children: ReactNode;
  className?: string;
}

export function GradientHeading({ as: Tag = 'h2', children, className }: GradientHeadingProps) {
  return (
    <Tag
      className={cn('gradient-heading font-heading font-bold', className)}
    >
      {children}
    </Tag>
  );
}
