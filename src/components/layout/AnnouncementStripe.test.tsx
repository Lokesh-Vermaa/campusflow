import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { AnnouncementStripe } from '@/components/layout/AnnouncementStripe';

describe('AnnouncementStripe', () => {
  const defaultProps = {
    message: 'Important campus announcement here.',
    linkText: 'Learn More',
    linkHref: '/announcements',
  };

  it('renders message text', () => {
    render(<AnnouncementStripe {...defaultProps} />);
    expect(screen.getByText(/important campus announcement here/i)).toBeInTheDocument();
  });

  it('renders action link with correct href', () => {
    render(<AnnouncementStripe {...defaultProps} />);
    const link = screen.getByRole('link', { name: /learn more/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', '/announcements');
  });

  it('renders dismiss button when onDismiss provided', () => {
    const onDismiss = vi.fn();
    render(<AnnouncementStripe {...defaultProps} onDismiss={onDismiss} />);
    expect(screen.getByRole('button', { name: /dismiss announcement/i })).toBeInTheDocument();
  });

  it('calls onDismiss when dismiss button clicked', async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    render(<AnnouncementStripe {...defaultProps} onDismiss={onDismiss} />);
    await user.click(screen.getByRole('button', { name: /dismiss announcement/i }));
    expect(onDismiss).toHaveBeenCalledOnce();
  });

  it('does not render dismiss button when onDismiss not provided', () => {
    render(<AnnouncementStripe {...defaultProps} />);
    expect(screen.queryByRole('button', { name: /dismiss announcement/i })).not.toBeInTheDocument();
  });
});
