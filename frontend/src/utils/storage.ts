/**
 * Secure token storage utilities
 */
export class TokenManager {
  private static readonly SESSION_TOKEN_KEY = 'session_token';
  private static readonly ACCESS_TOKEN_KEY = 'access_token';

  static setSessionToken(token: string): void {
    sessionStorage.setItem(this.SESSION_TOKEN_KEY, token);
  }

  static getSessionToken(): string | null {
    return sessionStorage.getItem(this.SESSION_TOKEN_KEY);
  }

  static setAccessToken(token: string): void {
    sessionStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  static getAccessToken(): string | null {
    return sessionStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  static clearTokens(): void {
    sessionStorage.removeItem(this.SESSION_TOKEN_KEY);
    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
  }

  static hasValidTokens(): boolean {
    return !!(this.getSessionToken() && this.getAccessToken());
  }
}