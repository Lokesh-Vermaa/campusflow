import { forwardRef, type ButtonHTMLAttributes } from 'react';
import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/cn';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default:
          'bg-brand-azure-strong text-white hover:bg-brand-azure focus-visible:outline-brand-azure-strong',
        destructive:
          'bg-red-600 text-white hover:bg-red-700 focus-visible:outline-red-600',
        outline:
          'border border-border-color bg-surface text-text-body hover:bg-surface-alt focus-visible:outline-brand-azure-strong',
        secondary:
          'bg-surface-alt text-text-heading hover:bg-border-color focus-visible:outline-brand-azure-strong',
        ghost:
          'text-text-body hover:bg-surface-alt focus-visible:outline-brand-azure-strong',
        link: 'text-brand-azure-strong underline-offset-4 hover:underline focus-visible:outline-brand-azure-strong',
        orange:
          'bg-brand-orange text-text-heading hover:bg-brand-orange-ink hover:text-white focus-visible:outline-brand-orange',
      },
      size: {
        default: 'h-9 px-4 py-2',
        sm: 'h-8 rounded-md px-3 text-xs',
        lg: 'h-11 rounded-md px-8',
        icon: 'h-9 w-9',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
);

export interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button';
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    );
  },
);

Button.displayName = 'Button';

// eslint-disable-next-line react-refresh/only-export-components
export { Button, buttonVariants };
