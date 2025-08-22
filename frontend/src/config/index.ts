import type { ApiConfig, OidcConfig } from '@/types';

export const apiConfig: ApiConfig = {
  baseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
  retryAttempts: 3,
};

export const oidcConfig: OidcConfig = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY || '',
  clientId: import.meta.env.VITE_OIDC_CLIENT_ID || '',
  redirectUri: `${window.location.origin}/callback`,
  scope: 'openid profile email',
  responseType: 'code',
  usePkce: true,
};