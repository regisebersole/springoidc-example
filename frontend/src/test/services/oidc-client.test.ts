import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { OidcClient } from '@/services/oidc-client';
import type { OidcConfig } from '@/types';

// Mock fetch globally
global.fetch = vi.fn();

const mockConfig: OidcConfig = {
  authority: 'https://accounts.google.com',
  clientId: 'test-client-id',
  redirectUri: 'http://localhost:3000/callback',
  scope: 'openid profile email',
  responseType: 'code',
  usePkce: true,
};

const mockDiscoveryDocument = {
  authorization_endpoint: 'https://accounts.google.com/o/oauth2/v2/auth',
  token_endpoint: 'https://oauth2.googleapis.com/token',
  end_session_endpoint: 'https://accounts.google.com/logout',
  issuer: 'https://accounts.google.com',
};

describe('OidcClient - Core PKCE Functionality', () => {
  let mockFetch: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mockFetch = vi.mocked(fetch);
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    localStorage.clear();
  });

  describe('PKCE Challenge Generation', () => {
    it('should generate PKCE challenge with correct format', async () => {
      // Requirement 9.1: Generate PKCE code verifier and challenge
      const oidcClient = new OidcClient(mockConfig);
      const challenge = await oidcClient.generatePkceChallenge();

      expect(challenge).toHaveProperty('codeVerifier');
      expect(challenge).toHaveProperty('codeChallenge');
      expect(challenge).toHaveProperty('codeChallengeMethod', 'S256');
      expect(challenge.codeVerifier).toHaveLength(128);
      expect(challenge.codeChallenge).toBeTruthy();
      expect(challenge.codeChallenge).not.toBe(challenge.codeVerifier);
    });

    it('should generate different challenges on each call', async () => {
      const oidcClient = new OidcClient(mockConfig);
      const challenge1 = await oidcClient.generatePkceChallenge();
      const challenge2 = await oidcClient.generatePkceChallenge();

      expect(challenge1.codeVerifier).not.toBe(challenge2.codeVerifier);
      expect(challenge1.codeChallenge).not.toBe(challenge2.codeChallenge);
    });
  });

  describe('Authorization Request Creation', () => {
    it('should create authorization request with PKCE challenge', async () => {
      // Requirement 9.2: Include PKCE challenge in authorization request
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockDiscoveryDocument),
      } as Response);

      const oidcClient = new OidcClient(mockConfig);
      const authRequest = await oidcClient.createAuthorizationRequest();

      expect(authRequest.authorizationUrl).toContain('code_challenge=');
      expect(authRequest.authorizationUrl).toContain('code_challenge_method=S256');
      expect(authRequest.authorizationUrl).toContain('response_type=code');
      expect(authRequest.authorizationUrl).toContain(`client_id=${mockConfig.clientId}`);
    });
  });

  describe('Token Exchange', () => {
    it('should exchange authorization code for tokens using PKCE verifier', async () => {
      // Requirement 9.3: Exchange authorization code for tokens using PKCE code verifier
      const mockTokenResponse = {
        access_token: 'mock-access-token',
        token_type: 'Bearer',
        expires_in: 3600,
      };

      // Mock discovery document fetch
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockDiscoveryDocument),
      } as Response);

      // Mock token exchange response
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockTokenResponse),
      } as Response);

      // Mock stored state
      vi.spyOn(sessionStorage, 'getItem').mockReturnValue('test-state');

      const oidcClient = new OidcClient(mockConfig);
      const tokenRequest = {
        code: 'test-auth-code',
        state: 'test-state',
        codeVerifier: 'test-code-verifier',
      };

      const tokens = await oidcClient.exchangeCodeForTokens(tokenRequest);

      expect(tokens).toEqual(mockTokenResponse);
      
      // Verify the token endpoint was called
      expect(mockFetch).toHaveBeenCalledWith(
        mockDiscoveryDocument.token_endpoint,
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        })
      );
    });

    it('should validate state parameter to prevent CSRF attacks', async () => {
      // Mock discovery document fetch
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockDiscoveryDocument),
      } as Response);

      // Mock stored state that doesn't match
      vi.spyOn(sessionStorage, 'getItem').mockReturnValue('different-state');

      const oidcClient = new OidcClient(mockConfig);
      const tokenRequest = {
        code: 'test-auth-code',
        state: 'test-state',
        codeVerifier: 'test-code-verifier',
      };

      await expect(oidcClient.exchangeCodeForTokens(tokenRequest)).rejects.toThrow(
        'Invalid state parameter - possible CSRF attack'
      );
    });
  });

  describe('Token Refresh', () => {
    it('should refresh access token using refresh token', async () => {
      // Requirement 9.4: Refresh tokens automatically
      const mockRefreshResponse = {
        access_token: 'new-access-token',
        token_type: 'Bearer',
        expires_in: 3600,
      };

      // Mock discovery document fetch
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockDiscoveryDocument),
      } as Response);

      // Mock refresh token response
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockRefreshResponse),
      } as Response);

      const oidcClient = new OidcClient(mockConfig);
      const tokens = await oidcClient.refreshToken('mock-refresh-token');

      expect(tokens).toEqual(mockRefreshResponse);
      
      // Verify the token endpoint was called with refresh grant
      expect(mockFetch).toHaveBeenCalledWith(
        mockDiscoveryDocument.token_endpoint,
        expect.objectContaining({
          method: 'POST',
          body: expect.stringContaining('grant_type=refresh_token'),
        })
      );
    });
  });

  describe('Callback Handling', () => {
    it('should parse successful authorization callback', () => {
      const oidcClient = new OidcClient(mockConfig);
      const callbackUrl = 'http://localhost:3000/callback?code=test-code&state=test-state';
      
      const result = oidcClient.parseAuthorizationCallback(callbackUrl);

      expect(result).toEqual({
        code: 'test-code',
        state: 'test-state',
      });
    });

    it('should handle authorization error in callback', () => {
      const oidcClient = new OidcClient(mockConfig);
      const callbackUrl = 'http://localhost:3000/callback?error=access_denied&error_description=User%20denied%20access';

      expect(() => oidcClient.parseAuthorizationCallback(callbackUrl)).toThrow(
        'Authorization failed: access_denied - User denied access'
      );
    });

    it('should complete token exchange in callback', async () => {
      // Requirement 9.3: Complete token exchange in callback
      const mockTokenResponse = {
        access_token: 'mock-access-token',
        token_type: 'Bearer',
        expires_in: 3600,
      };

      // Mock discovery document fetch
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockDiscoveryDocument),
      } as Response);

      // Mock token exchange response
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockTokenResponse),
      } as Response);

      // Mock stored values
      vi.spyOn(sessionStorage, 'getItem').mockImplementation((key) => {
        if (key === 'oidc_state') return 'test-state';
        if (key === 'pkce_code_verifier') return 'test-code-verifier';
        return null;
      });

      const oidcClient = new OidcClient(mockConfig);
      const callbackUrl = 'http://localhost:3000/callback?code=test-code&state=test-state';

      const tokens = await oidcClient.handleCallback(callbackUrl);

      expect(tokens).toHaveProperty('access_token', 'mock-access-token');
      expect(tokens).toHaveProperty('token_type', 'Bearer');
    });
  });

  describe('Login Flow', () => {
    it('should initiate OIDC login flow with PKCE', async () => {
      // Requirement 9.1: Initiate login with PKCE flow
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockDiscoveryDocument),
      } as Response);

      const originalLocation = window.location;
      delete (window as any).location;
      window.location = { ...originalLocation, href: '' };

      const oidcClient = new OidcClient(mockConfig);
      await oidcClient.login();

      expect(window.location.href).toContain(mockDiscoveryDocument.authorization_endpoint);
      expect(window.location.href).toContain('code_challenge=');
      expect(window.location.href).toContain('code_challenge_method=S256');

      window.location = originalLocation;
    });
  });

  describe('Logout', () => {
    it('should logout and clean up session', async () => {
      // Requirement 9.6: Clear tokens and redirect to OIDC provider logout
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockDiscoveryDocument),
      } as Response);

      const removeItemSpy = vi.spyOn(sessionStorage, 'removeItem');
      const localStorageRemoveSpy = vi.spyOn(localStorage, 'removeItem');
      
      const originalLocation = window.location;
      delete (window as any).location;
      window.location = { ...originalLocation, href: '', origin: 'http://localhost:3000' };

      const oidcClient = new OidcClient(mockConfig);
      await oidcClient.logout('mock-id-token');

      // Verify session cleanup
      expect(removeItemSpy).toHaveBeenCalledWith('pkce_code_verifier');
      expect(removeItemSpy).toHaveBeenCalledWith('oidc_state');
      expect(removeItemSpy).toHaveBeenCalledWith('oidc_nonce');
      expect(localStorageRemoveSpy).toHaveBeenCalledWith('access_token');
      expect(localStorageRemoveSpy).toHaveBeenCalledWith('refresh_token');
      expect(localStorageRemoveSpy).toHaveBeenCalledWith('id_token');

      // Verify redirect to OIDC provider logout
      expect(window.location.href).toContain(mockDiscoveryDocument.end_session_endpoint);
      expect(window.location.href).toContain('id_token_hint=mock-id-token');

      window.location = originalLocation;
    });
  });
});