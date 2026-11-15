import Markdown from 'react-markdown';
import rehypeSanitize from 'rehype-sanitize';

interface AgentMessageRendererProps {
  content: string;
}

/**
 * Renders untrusted LLM markdown safely using rehype-sanitize.
 * Never executes raw HTML or script tags.
 */
export function AgentMessageRenderer({ content }: AgentMessageRendererProps) {
  return (
    <div className="prose prose-sm max-w-none text-text-body">
      <Markdown
        rehypePlugins={[rehypeSanitize]}
        components={{
          a: ({ children, ...props }) => (
            <a
              {...props}
              target="_blank"
              rel="noopener noreferrer"
              className="text-brand-azure-strong underline hover:text-brand-azure"
            >
              {children}
            </a>
          ),
          p: ({ children }) => <p className="mb-2 last:mb-0 leading-relaxed">{children}</p>,
          ul: ({ children }) => <ul className="mb-2 ml-4 list-disc space-y-1">{children}</ul>,
          ol: ({ children }) => <ol className="mb-2 ml-4 list-decimal space-y-1">{children}</ol>,
          strong: ({ children }) => <strong className="font-semibold text-text-heading">{children}</strong>,
          code: ({ children }) => (
            <code className="rounded bg-surface-alt px-1.5 py-0.5 font-mono text-xs text-brand-azure-strong border border-border-color">
              {children}
            </code>
          ),
        }}
      >
        {content}
      </Markdown>
    </div>
  );
}

