import { oidcConfig } from '@/config';
import { OidcClient, type TokenResponse } from './oidc-client';
import type { UserInfo, AuthResponse } from '@/types';

export class AuthService {
  private oidcClient: OidcClient;
  private currentTokens: TokenResponse | null = null;

  constructor() {
    this.oidcClient = new OidcClient(oidcConfig);
  }

  /**
   * Initiate OIDC login flow with PKCE
   * Requirements: 9.1 - Initiate login with PKCE flow
   */
  async signIn(): Promise<void> {
    await this.oidcClient.login();
  }

  /**
   * Handle login callback and complete token exchange
   * Requirements: 9.3 - Complete token exchange in callback
   */
  async signInCallback(callbackUrl?: string): Promise<TokenResponse> {
    const tokens = await this.oidcClient.handleCallback(callbackUrl);
    this.currentTokens = tokens;
    
    // Store tokens securely
    this.storeTokens(tokens);
    
    return tokens;
  }

  /**
   * Sign out and clean up session
   * Requirements: 9.6 - Clear tokens and redirect to OIDC provider logout
   */
  async signOut(): Promise<void> {
    const idToken = this.currentTokens?.id_token;
    this.clearTokens();
    await this.oidcClient.logout(idToken);
  }

  /**
   * Get current access token
   */
  async getAccessToken(): Promise<string | null> {
    if (this.currentTokens?.access_token) {
      // Check if token is expired and refresh if needed
      if (this.isTokenExpired(this.currentTokens)) {
        await this.refreshTokens();
      }
      return this.currentTokens.access_token;
    }

    // Try to load from storage
    const storedTokens = this.loadTokensFromStorage();
    if (storedTokens) {
      this.currentTokens = storedTokens;
      if (this.isTokenExpired(storedTokens)) {
        await this.refreshTokens();
      }
      return this.currentTokens?.access_token || null;
    }

    return null;
  }

  /**
   * Check if user is authenticated
   */
  async isAuthenticated(): Promise<boolean> {
    const accessToken = await this.getAccessToken();
    return accessToken !== null;
  }

  /**
   * Refresh access token using refresh token
   * Requirements: 9.4 - Refresh tokens automatically
   */
  private async refreshTokens(): Promise<void> {
    if (!this.currentTokens?.refresh_token) {
      throw new Error('No refresh token available');
    }

    try {
      const newTokens = await this.oidcClient.refreshToken(this.currentTokens.refresh_token);
      this.currentTokens = { ...this.currentTokens, ...newTokens };
      this.storeTokens(this.currentTokens);
    } catch (error) {
      // Refresh failed, clear tokens and require re-authentication
      this.clearTokens();
      throw error;
    }
  }

  /**
   * Store tokens securely in localStorage
   */
  private storeTokens(tokens: TokenResponse): void {
    localStorage.setItem('access_token', tokens.access_token);
    localStorage.setItem('token_type', tokens.token_type);
    localStorage.setItem('expires_in', tokens.expires_in.toString());
    localStorage.setItem('token_received_at', Date.now().toString());
    
    if (tokens.refresh_token) {
      localStorage.setItem('refresh_token', tokens.refresh_token);
    }
    if (tokens.id_token) {
      localStorage.setItem('id_token', tokens.id_token);
    }
  }

  /**
   * Load tokens from localStorage
   */
  private loadTokensFromStorage(): TokenResponse | null {
    const accessToken = localStorage.getItem('access_token');
    const tokenType = localStorage.getItem('token_type');
    const expiresIn = localStorage.getItem('expires_in');
    
    if (!accessToken || !tokenType || !expiresIn) {
      return null;
    }

    return {
      access_token: accessToken,
      token_type: tokenType,
      expires_in: parseInt(expiresIn, 10),
      refresh_token: localStorage.getItem('refresh_token') || undefined,
      id_token: localStorage.getItem('id_token') || undefined,
    };
  }

  /**
   * Check if token is expired
   */
  private isTokenExpired(tokens: TokenResponse): boolean {
    const receivedAt = localStorage.getItem('token_received_at');
    if (!receivedAt) {
      return true;
    }

    const expirationTime = parseInt(receivedAt, 10) + (tokens.expires_in * 1000);
    const now = Date.now();
    
    // Consider token expired if it expires within the next 5 minutes
    return now >= (expirationTime - 5 * 60 * 1000);
  }

  /**
   * Clear all stored tokens
   */
  private clearTokens(): void {
    this.currentTokens = null;
    localStorage.removeItem('access_token');
    localStorage.removeItem('token_type');
    localStorage.removeItem('expires_in');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('id_token');
    localStorage.removeItem('token_received_at');
  }

  // This method will be enhanced in later tasks to exchange tokens with backend
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  async exchangeTokenWithBackend(_accessToken: string): Promise<AuthResponse> {
    // Placeholder implementation - will be completed in task 15
    throw new Error('Token exchange not implemented yet');
  }

  /**
   * Extract user info from ID token (basic implementation)
   * This is a simplified version - in production you'd properly decode and validate the JWT
   */
  extractUserInfoFromIdToken(idToken: string): UserInfo | null {
    try {
      // Basic JWT payload extraction (not secure for production)
      const payload = idToken.split('.')[1];
      const decoded = JSON.parse(atob(payload));
      
      return {
        userId: decoded.sub || '',
        email: decoded.email || '',
        name: decoded.name || decoded.given_name + ' ' + decoded.family_name || '',
        roles: decoded.roles || [],
      };
    } catch (error) {
      console.error('Failed to extract user info from ID token:', error);
      return null;
    }
  }

  /**
   * Get current user info
   */
  async getCurrentUser(): Promise<UserInfo | null> {
    if (this.currentTokens?.id_token) {
      return this.extractUserInfoFromIdToken(this.currentTokens.id_token);
    }

    const storedIdToken = localStorage.getItem('id_token');
    if (storedIdToken) {
      return this.extractUserInfoFromIdToken(storedIdToken);
    }

    return null;
  }
}

export const authService = new AuthService();