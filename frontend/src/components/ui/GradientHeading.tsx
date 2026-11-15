import { type HTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

interface GradientHeadingProps extends HTMLAttributes<HTMLHeadingElement> {
  as?: 'h1' | 'h2' | 'h3';
}

export function GradientHeading({ as: Tag = 'h2', children, className, ...props }: GradientHeadingProps) {
  return (
    <Tag
      className={cn('gradient-heading font-heading font-bold', className)}
      {...props}
    >
      {children}
    </Tag>
  );
}
