// Smart configuration for Service Virtualization Platform
// Auto-detects deployment context - works on shared domains and dedicated domains

/**
 * Detects the URL path prefix for the current deployment.
 * Used both for API URL construction and BrowserRouter basename.
 *
 * Deployment patterns:
 *   /margin/      → '/margin'
 *   /collateral/  → '/collateral'
 *   /sv/          → '/sv'  (legacy)
 *   /             → '/'   (dedicated domain or local dev)
 */
export const getBasePath = (): string => {
  const currentPath = window.location.pathname;
  if (currentPath.startsWith('/margin')) return '/margin';
  if (currentPath.startsWith('/collateral')) return '/collateral';
  if (currentPath.startsWith('/sv')) return '/sv';
  return '/';
};

/**
 * Automatically detects the correct API base URL based on current page location.
 *
 * Supported deployment patterns:
 * - Istio shared domain (AKS): https://app.mycompany.com/margin/     → /margin/api
 * - Istio shared domain (AKS): https://app.mycompany.com/collateral/ → /collateral/api
 * - Dedicated domain:          https://sv.mycompany.com/             → /api
 * - Local development:         http://localhost:3000/                → /api
 *
 * In all cases nginx proxies /api/* → backend as a local docker fallback.
 * In Istio deployments the browser sends the full prefixed path (e.g. /margin/api/*)
 * which Istio routes directly to the backend service — nginx is never involved.
 */
const getApiBaseUrl = (): string => {
  const basePath = getBasePath();
  return basePath === '/' ? '/api' : `${basePath}/api`;
};

const config = {
  API_URL: getApiBaseUrl()
};

export default config;
