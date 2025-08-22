import type { OidcConfig } from '@/types';

export interface PkceChallenge {
  codeVerifier: string;
  codeChallenge: string;
  codeChallengeMethod: string;
}

export interface AuthorizationRequest {
  authorizationUrl: string;
  state: string;
  nonce: string;
  codeVerifier: string;
}

export interface TokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  refresh_token?: string;
  id_token?: string;
  scope?: string;
}

export interface TokenExchangeRequest {
  code: string;
  codeVerifier: string;
  state: string;
}

/**
 * OIDC Client with PKCE (Proof Key for Code Exchange) implementation
 * Implements RFC 7636 for secure authorization code flow
 */
export class OidcClient {
  private config: OidcConfig;
  private discoveryDocument: any = null;

  constructor(config: OidcConfig) {
    this.config = config;
  }

  /**
   * Generate cryptographically secure random string for PKCE
   */
  private generateRandomString(length: number): string {
    const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
    const array = new Uint8Array(length);
    crypto.getRandomValues(array);
    return Array.from(array, byte => charset[byte % charset.length]).join('');
  }

  /**
   * Generate SHA256 hash and base64url encode
   */
  private async sha256(plain: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(plain);
    const hash = await crypto.subtle.digest('SHA-256', data);
    return this.base64UrlEncode(new Uint8Array(hash));
  }

  /**
   * Base64URL encode without padding
   */
  private base64UrlEncode(array: Uint8Array): string {
    return btoa(String.fromCharCode(...array))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '');
  }

  /**
   * Generate PKCE code verifier and challenge
   * Requirements: 9.1 - Generate PKCE code verifier and challenge
   */
  async generatePkceChallenge(): Promise<PkceChallenge> {
    // Generate code verifier (43-128 characters)
    const codeVerifier = this.generateRandomString(128);
    
    // Generate code challenge using SHA256
    const codeChallenge = await this.sha256(codeVerifier);
    
    return {
      codeVerifier,
      codeChallenge,
      codeChallengeMethod: 'S256'
    };
  }

  /**
   * Generate state and nonce for security
   */
  private generateSecurityTokens(): { state: string; nonce: string } {
    return {
      state: this.generateRandomString(32),
      nonce: this.generateRandomString(32)
    };
  }

  /**
   * Discover OIDC provider endpoints
   */
  private async getDiscoveryDocument(): Promise<any> {
    if (this.discoveryDocument) {
      return this.discoveryDocument;
    }

    const discoveryUrl = `${this.config.authority}/.well-known/openid_configuration`;
    const response = await fetch(discoveryUrl);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch discovery document: ${response.statusText}`);
    }

    this.discoveryDocument = await response.json();
    return this.discoveryDocument;
  }

  /**
   * Create authorization request URL with PKCE challenge
   * Requirements: 9.2 - Include PKCE challenge in authorization request
   */
  async createAuthorizationRequest(): Promise<AuthorizationRequest> {
    const discovery = await this.getDiscoveryDocument();
    const pkce = await this.generatePkceChallenge();
    const { state, nonce } = this.generateSecurityTokens();

    const params = new URLSearchParams({
      response_type: this.config.responseType,
      client_id: this.config.clientId,
      redirect_uri: this.config.redirectUri,
      scope: this.config.scope,
      state,
      nonce,
      code_challenge: pkce.codeChallenge,
      code_challenge_method: pkce.codeChallengeMethod,
    });

    const authorizationUrl = `${discovery.authorization_endpoint}?${params.toString()}`;

    // Store PKCE verifier and state in session storage for later use
    sessionStorage.setItem('pkce_code_verifier', pkce.codeVerifier);
    sessionStorage.setItem('oidc_state', state);
    sessionStorage.setItem('oidc_nonce', nonce);

    return {
      authorizationUrl,
      state,
      nonce,
      codeVerifier: pkce.codeVerifier
    };
  }

  /**
   * Exchange authorization code for tokens using PKCE verifier
   * Requirements: 9.3 - Exchange authorization code for tokens using PKCE code verifier
   */
  async exchangeCodeForTokens(request: TokenExchangeRequest): Promise<TokenResponse> {
    const discovery = await this.getDiscoveryDocument();

    // Validate state parameter
    const storedState = sessionStorage.getItem('oidc_state');
    if (request.state !== storedState) {
      throw new Error('Invalid state parameter - possible CSRF attack');
    }

    const tokenRequestBody = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: this.config.clientId,
      code: request.code,
      redirect_uri: this.config.redirectUri,
      code_verifier: request.codeVerifier,
    });

    const response = await fetch(discovery.token_endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: tokenRequestBody.toString(),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(`Token exchange failed: ${errorData.error_description || response.statusText}`);
    }

    const tokenResponse: TokenResponse = await response.json();

    // Clean up stored values
    sessionStorage.removeItem('pkce_code_verifier');
    sessionStorage.removeItem('oidc_state');
    sessionStorage.removeItem('oidc_nonce');

    return tokenResponse;
  }

  /**
   * Handle authorization callback and extract code and state
   */
  parseAuthorizationCallback(url: string): { code: string; state: string } | null {
    const urlObj = new URL(url);
    const code = urlObj.searchParams.get('code');
    const state = urlObj.searchParams.get('state');
    const error = urlObj.searchParams.get('error');

    if (error) {
      const errorDescription = urlObj.searchParams.get('error_description');
      throw new Error(`Authorization failed: ${error} - ${errorDescription || 'Unknown error'}`);
    }

    if (!code || !state) {
      return null;
    }

    return { code, state };
  }

  /**
   * Initiate OIDC login flow with PKCE
   * Requirements: 9.1 - Initiate login with PKCE flow
   */
  async login(): Promise<void> {
    const authRequest = await this.createAuthorizationRequest();
    window.location.href = authRequest.authorizationUrl;
  }

  /**
   * Handle login callback and complete token exchange
   * Requirements: 9.3 - Complete token exchange in callback
   */
  async handleCallback(callbackUrl: string = window.location.href): Promise<TokenResponse> {
    const callbackData = this.parseAuthorizationCallback(callbackUrl);
    
    if (!callbackData) {
      throw new Error('Invalid callback URL - missing code or state');
    }

    const storedCodeVerifier = sessionStorage.getItem('pkce_code_verifier');
    if (!storedCodeVerifier) {
      throw new Error('Missing PKCE code verifier - possible session timeout');
    }

    return await this.exchangeCodeForTokens({
      code: callbackData.code,
      state: callbackData.state,
      codeVerifier: storedCodeVerifier
    });
  }

  /**
   * Refresh access token using refresh token
   * Requirements: 9.4 - Refresh tokens automatically
   */
  async refreshToken(refreshToken: string): Promise<TokenResponse> {
    const discovery = await this.getDiscoveryDocument();

    const refreshRequestBody = new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: this.config.clientId,
      refresh_token: refreshToken,
    });

    const response = await fetch(discovery.token_endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: refreshRequestBody.toString(),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(`Token refresh failed: ${errorData.error_description || response.statusText}`);
    }

    return await response.json();
  }

  /**
   * Logout and clean up session
   * Requirements: 9.6 - Clear tokens and redirect to OIDC provider logout
   */
  async logout(idToken?: string): Promise<void> {
    const discovery = await this.getDiscoveryDocument();
    
    // Clean up any stored session data
    sessionStorage.removeItem('pkce_code_verifier');
    sessionStorage.removeItem('oidc_state');
    sessionStorage.removeItem('oidc_nonce');
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('id_token');

    // Redirect to OIDC provider logout if end_session_endpoint is available
    if (discovery.end_session_endpoint) {
      const logoutParams = new URLSearchParams({
        post_logout_redirect_uri: window.location.origin,
      });

      if (idToken) {
        logoutParams.set('id_token_hint', idToken);
      }

      window.location.href = `${discovery.end_session_endpoint}?${logoutParams.toString()}`;
    } else {
      // Fallback: just redirect to home page
      window.location.href = window.location.origin;
    }
  }
}