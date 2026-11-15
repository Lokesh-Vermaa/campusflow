import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { AgentMessageRenderer } from './AgentMessageRenderer';

describe('AgentMessageRenderer (Security & Sanitization)', () => {
  it('renders standard markdown formatting safely', () => {
    const markdown = '**Important Update:** Please visit [Booking Rules](https://demo.jklu.test/rules).';
    render(<AgentMessageRenderer content={markdown} />);

    expect(screen.getByText(/important update:/i)).toBeInTheDocument();
    const link = screen.getByRole('link', { name: /booking rules/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('sanitizes and DOES NOT execute raw HTML script tags (XSS Prevention)', () => {
    const maliciousPayload =
      'Safe text <script>alert("XSS Attack");</script> <img src="x" onerror="alert(1)" /> after text';

    const { container } = render(<AgentMessageRenderer content={maliciousPayload} />);

    // <script> tags must be stripped by rehype-sanitize
    expect(container.querySelector('script')).toBeNull();

    // Dangerous onerror attributes must not be present
    const img = container.querySelector('img');
    if (img) {
      expect(img.getAttribute('onerror')).toBeNull();
    }

    expect(screen.getByText(/safe text/i)).toBeInTheDocument();
  });

  it('renders formatted lists and code blocks correctly', () => {
    const listContent = `
- Item 1: Free Room
- Item 2: Guest House
\`inline_code\`
    `;

    render(<AgentMessageRenderer content={listContent} />);
    expect(screen.getByText(/item 1: free room/i)).toBeInTheDocument();
    expect(screen.getByText('inline_code')).toBeInTheDocument();
  });
});
