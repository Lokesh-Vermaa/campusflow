# JKLU CampusFlow Security & Accessibility Checklist

This document details the security posture, defensive programming patterns, and WCAG AA accessibility compliance implemented across **JKLU CampusFlow** (AI-Buildathon 1.0, Problem Statement PS-02).

---

## 1. Security Architecture & Threat Mitigation

### 1.1 Zero Secrets in Client Bundle
- **Enforcement**: Build artifacts inspected. Zero private keys, API secrets, service account credentials, or database connection strings exist in client source code.
- **Environment Boundaries**: Only public configuration prefixed with `VITE_` (such as `VITE_API_BASE_URL` and `VITE_USE_MOCKS`) is exposed via `import.meta.env`.
- **Compiler Hardening**: Production build strips all `console.*` and `debugger` statements via Vite/esbuild configuration to prevent runtime state leakage in browser devtools.

### 1.2 Zero Tracking, Zero Third-Party CDNs, Self-Hosted Assets
- **Asset Privacy**: Zero external scripts, Google Fonts, CDN stylesheets, or tracking pixels (`ga.js`, `gtag`, Segment, Hotjar, etc.).
- **Self-Hosted Typography**: Fonts (`Inter` and `Poppins`) are self-hosted using `@fontsource/inter` and `@fontsource/poppins`, eliminating third-party tracking or IP leakage to external CDNs.
- **Zero Remote Images**: All icons are rendered as inline SVG components via `lucide-react`. Logos are rendered via local SVGs.

### 1.3 Session Cookie & Authentication Integrity
- **Credential Modes**: `fetch` calls through `src/api/client.ts` specify `credentials: 'same-origin'` (or `'include'`), requiring the Spring Boot backend to issue `HttpOnly; Secure; SameSite=Strict` session cookies.
- **Local Storage Isolation**: Sensitive tokens are never stored in `localStorage` or `sessionStorage` where they would be vulnerable to Cross-Site Scripting (XSS).
- **Graceful Session Expiry**: 401 Unauthorized responses trigger a broadcast custom event (`auth:unauthorized`), clearing client auth state and redirecting to `/login` with clean state.

### 1.4 CSRF Protection (`X-CSRF-TOKEN`)
- **Defense-in-Depth**: State-changing HTTP methods (`POST`, `PUT`, `PATCH`, `DELETE`) inspect `document.cookie` for `XSRF-TOKEN` or `CSRF-TOKEN`.
- **Header Injection**: If present, the token is automatically injected into the `X-CSRF-TOKEN` request header.
- **Idempotency Protection**: Mutating booking requests include a unique `Idempotency-Key` (UUIDv4) header, preventing double-booking on accidental multi-clicks or network retries.

### 1.5 Anti-Enumeration: Identical 403 / 404 Handling
- **Information Leakage Prevention**: When an unauthorized user attempts to navigate to a protected admin route or a non-existent route, the application renders the unified `NotFoundOrForbidden` component:
  > *"Page not found or you do not have permission to view this resource."*
- **Role Concealment**: Attackers cannot determine whether a secret endpoint exists simply by observing HTTP status differences (404 vs 403). Both show an identical layout and message.

### 1.6 Strict Markdown Sanitization (LLM Output Defense)
- **Untrusted Content Policy**: AI Agent responses are treated as inherently untrusted user-controlled input.
- **Sanitization Pipeline**: All assistant markdown is processed through `react-markdown` with `rehype-sanitize`.
- **Zero Raw HTML**: `dangerouslySetInnerHTML` is prohibited and absent from the entire codebase. Malicious payloads like `<script>`, `onerror=`, `<iframe>`, and javascript: URIs are stripped before rendering.

### 1.7 Client-Side Schema Validation via Zod
- **Boundary Verification**: Every inbound API payload and outbound form submission is validated at the boundary with Zod schemas (`src/api/types.ts`).
- **Malformed Data Rejection**: Malformed or unexpected backend payloads fail fast with typed `ApiError` instances rather than producing undefined runtime crashes.
- **Form Hardening**: Forms use `react-hook-form` paired with `@hookform/resolvers/zod` to prevent invalid or out-of-range payloads from reaching the network.

### 1.8 Error Boundary with Traceable `requestId`
- **Blast Radius Containment**: A global React `ErrorBoundary` wraps the entire component tree (`src/components/common/ErrorBoundary.tsx`).
- **Telemetry & Support**: Unhandled React render crashes display a user-friendly error screen with an incident `requestId` for support triage, alongside a "Return to Dashboard" recovery action.

---

## 2. Production Security Headers & Content-Security-Policy (CSP)

Deployments serving CampusFlow must configure the following HTTP response headers at the reverse proxy (Nginx / Cloudflare / Spring Boot Gateway):

```http
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self'; connect-src 'self' https://api.jklu.edu.in; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self';
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
```

### Explanation of Directives:
- `default-src 'self'`: Restricts all resources to the application origin by default.
- `script-src 'self'`: Disallows external scripts and arbitrary eval.
- `connect-src 'self' https://api.jklu.edu.in`: Restricts API and SSE network requests strictly to trusted backend endpoints.
- `object-src 'none'`: Blocks Flash, Java applets, and other plugins.
- `frame-ancestors 'none'`: Mitigates clickjacking attacks.
- `X-Content-Type-Options: nosniff`: Prevents MIME-type sniffing.

---

## 3. WCAG AA Color Contrast Matrix

JKLU CampusFlow strictly adheres to WCAG 2.1 AA requirements (minimum **4.5:1** contrast ratio for normal text and **3.0:1** for large text / UI graphical components).

All colors are governed exclusively through design tokens in `src/styles/tokens.css` and the Tailwind theme.

### Contrast Ratio Table

| Foreground Token / Color | Background Token / Color | Calculated Contrast | WCAG AA Requirement | Compliance Status | Usage Context & Rules |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **White** (`#FFFFFF`) | `--brand-periwinkle-deep` (`#4A67B0`) | **5.44 : 1** | Normal Text (≥ 4.5:1) | **PASS** | Top announcement stripe, hero banners, feature bands, dark button labels |
| **White** (`#FFFFFF`) | `--brand-azure-strong` (`#0069C9`) | **5.43 : 1** | Normal Text (≥ 4.5:1) | **PASS** | Floating dock, primary links, action buttons |
| **White** (`#FFFFFF`) | `--brand-orange-ink` (`#C74A00`) | **4.76 : 1** | Normal Text (≥ 4.5:1) | **PASS** | High-contrast orange badges, alert pills |
| `--text-body` (`#212529`) | `--surface` (`#FFFFFF`) | **15.43 : 1** | Normal Text (≥ 4.5:1) | **PASS (AAA)** | Primary paragraph text on white cards |
| `--text-heading` (`#111827`) | `--surface` (`#FFFFFF`) | **17.74 : 1** | Normal Text (≥ 4.5:1) | **PASS (AAA)** | Section titles, card headers, table data |
| `--text-body` (`#212529`) | `--surface-alt` (`#F7F7F7`) | **14.40 : 1** | Normal Text (≥ 4.5:1) | **PASS (AAA)** | Secondary section descriptions |
| `--text-heading` (`#111827`) | `--surface-alt` (`#F7F7F7`) | **16.56 : 1** | Normal Text (≥ 4.5:1) | **PASS (AAA)** | Section headings on alternate gray surface |
| `--brand-orange-ink` (`#C74A00`) | `--surface` (`#FFFFFF`) | **4.76 : 1** | Normal Text (≥ 4.5:1) | **PASS** | Orange text or icons on white surfaces |
| `--brand-azure-strong` (`#0069C9`) | `--surface` (`#FFFFFF`) | **5.43 : 1** | Normal Text (≥ 4.5:1) | **PASS** | In-app hyperlinks and icon accents on white |
| `--text-heading` (`#111827`) | `--brand-orange` (`#FF6B00`) | **6.21 : 1** | Normal Text (≥ 4.5:1) | **PASS** | Primary CTA button label ("NEW BOOKING", "Submit") |
| `--text-body` (`#212529`) | `--brand-orange` (`#FF6B00`) | **5.40 : 1** | Normal Text (≥ 4.5:1) | **PASS** | Secondary text on orange surfaces |
| *White (`#FFFFFF`)* | *`--brand-orange` (`#FF6B00`)* | *2.86 : 1* | *Normal Text (≥ 4.5:1)* | **PROHIBITED** | **Strictly forbidden** across the codebase. Only dark text (`#111827`) is allowed on `--brand-orange`. |
| *White (`#FFFFFF`)* | *`--brand-periwinkle` (`#6B87C9`)* | *3.54 : 1* | *Normal Text (≥ 4.5:1)* | **RESTRICTED** | Permitted **only** for large display text (≥ 18pt / 24px) or decorative bands. |
| *White (`#FFFFFF`)* | *`--brand-azure` (`#0096FF`)* | *3.09 : 1* | *Normal Text (≥ 4.5:1)* | **RESTRICTED** | Permitted **only** for decorative glows and non-text graphical accents. |

---

## 4. Accessibility & Assistive Technology Features

1. **Semantic Landmarks**: Semantic `<header>`, `<nav>`, `<main id="main-content">`, `<section>`, `<aside>`, and `<footer>` elements used throughout.
2. **Skip-to-Content Link**: Focusable skip link (`Skip to main content`) renders as the very first focusable node on the page.
3. **Keyboard Navigation & Visible Focus**: Every interactive control possesses visible, high-contrast focus rings (`focus-visible:ring-2 focus-visible:ring-brand-azure-strong focus-visible:ring-offset-2`).
4. **Motion Sensitivity (`prefers-reduced-motion`)**: All scroll-reveal transitions and animations automatically disable when the user's operating system requests reduced motion.
5. **High-Contrast Fallbacks**: Text gradients (`--gradient-heading`) automatically fall back to solid `--brand-azure-strong` under Windows High Contrast / `forced-colors: active`.
6. **ARIA & Form Accessibility**: All icons have descriptive `aria-label`s or `aria-hidden="true"`. All form inputs have explicitly associated `<label>` tags with `htmlFor` bindings.

