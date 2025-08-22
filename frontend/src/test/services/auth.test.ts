import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { AuthService } from '@/services/auth';
import { OidcClient } from '@/services/oidc-client';

// Mock the OidcClient
vi.mock('@/services/oidc-client', () => ({
  OidcClient: vi.fn().mockImplementation(() => ({
    login: vi.fn(),
    handleCallback: vi.fn(),
    logout: vi.fn(),
    refreshToken: vi.fn(),
  })),
}));

describe('AuthService', () => {
  let authService: AuthService;
  let mockOidcClient: any;

  beforeEach(() => {
    // Clear localStorage
    localStorage.clear();
    vi.clearAllMocks();
    
    authService = new AuthService();
    mockOidcClient = (authService as any).oidcClient;
  });

  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('signIn', () => {
    it('should initiate OIDC login flow', async () => {
      await authService.signIn();
      
      expect(mockOidcClient.login).toHaveBeenCalledOnce();
    });
  });

  describe('signInCallback', () => {
    it('should handle callback and store tokens', async () => {
      const mockTokens = {
        access_token: 'mock-access-token',
        token_type: 'Bearer',
        expires_in: 3600,
        refresh_token: 'mock-refresh-token',
        id_token: 'mock-id-token',
      };

      mockOidcClient.handleCallback.mockResolvedValue(mockTokens);

      const result = await authService.signInCallback();

      expect(mockOidcClient.handleCallback).toHaveBeenCalledOnce();
      expect(result).toEqual(mockTokens);
      expect(localStorage.getItem('access_token')).toBe('mock-access-token');
      expect(localStorage.getItem('refresh_token')).toBe('mock-refresh-token');
    });
  });

  describe('signOut', () => {
    it('should clear tokens and logout', async () => {
      // Set up some tokens first
      localStorage.setItem('access_token', 'test-token');
      localStorage.setItem('id_token', 'test-id-token');

      await authService.signOut();

      expect(mockOidcClient.logout).toHaveBeenCalledWith(undefined);
      expect(localStorage.getItem('access_token')).toBe(null);
      expect(localStorage.getItem('id_token')).toBe(null);
    });
  });

  describe('getAccessToken', () => {
    it('should return access token from storage', async () => {
      localStorage.setItem('access_token', 'stored-token');
      localStorage.setItem('token_type', 'Bearer');
      localStorage.setItem('expires_in', '3600');
      localStorage.setItem('token_received_at', Date.now().toString());

      const token = await authService.getAccessToken();

      expect(token).toBe('stored-token');
    });

    it('should return null when no token is stored', async () => {
      const token = await authService.getAccessToken();

      expect(token).toBeNull();
    });
  });

  describe('isAuthenticated', () => {
    it('should return true when valid token exists', async () => {
      localStorage.setItem('access_token', 'valid-token');
      localStorage.setItem('token_type', 'Bearer');
      localStorage.setItem('expires_in', '3600');
      localStorage.setItem('token_received_at', Date.now().toString());

      const isAuth = await authService.isAuthenticated();

      expect(isAuth).toBe(true);
    });

    it('should return false when no token exists', async () => {
      const isAuth = await authService.isAuthenticated();

      expect(isAuth).toBe(false);
    });
  });

  describe('extractUserInfoFromIdToken', () => {
    it('should extract user info from valid ID token', () => {
      // Create a mock JWT payload
      const payload = {
        sub: 'user123',
        email: 'user@example.com',
        name: 'Test User',
        roles: ['user'],
      };
      
      // Create a mock JWT (header.payload.signature)
      const mockJwt = `header.${btoa(JSON.stringify(payload))}.signature`;

      const userInfo = authService.extractUserInfoFromIdToken(mockJwt);

      expect(userInfo).toEqual({
        userId: 'user123',
        email: 'user@example.com',
        name: 'Test User',
        roles: ['user'],
      });
    });

    it('should return null for invalid token', () => {
      const userInfo = authService.extractUserInfoFromIdToken('invalid-token');

      expect(userInfo).toBeNull();
    });
  });
});