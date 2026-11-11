import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { GradientHeading } from '@/components/ui/GradientHeading';

describe('GradientHeading', () => {
  it('renders as h2 by default', () => {
    render(<GradientHeading>Test Heading</GradientHeading>);
    const el = screen.getByRole('heading', { level: 2 });
    expect(el).toBeInTheDocument();
    expect(el).toHaveTextContent('Test Heading');
  });

  it('renders as h1 when specified', () => {
    render(<GradientHeading as="h1">Title</GradientHeading>);
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
  });

  it('renders as h3 when specified', () => {
    render(<GradientHeading as="h3">Section</GradientHeading>);
    expect(screen.getByRole('heading', { level: 3 })).toBeInTheDocument();
  });

  it('applies gradient-heading class', () => {
    render(<GradientHeading>Test</GradientHeading>);
    const el = screen.getByRole('heading', { level: 2 });
    expect(el).toHaveClass('gradient-heading');
  });

  it('merges additional className', () => {
    render(<GradientHeading className="text-4xl">Test</GradientHeading>);
    expect(screen.getByRole('heading')).toHaveClass('text-4xl');
  });
});
