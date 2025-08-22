import { User, UserManager, type UserManagerSettings } from 'oidc-client-ts';
import { oidcConfig } from '@/config';
import type { UserInfo, AuthResponse } from '@/types';

export class AuthService {
  private userManager: UserManager;

  constructor() {
    const settings: UserManagerSettings = {
      authority: oidcConfig.authority,
      client_id: oidcConfig.clientId,
      redirect_uri: oidcConfig.redirectUri,
      response_type: oidcConfig.responseType,
      scope: oidcConfig.scope,
      post_logout_redirect_uri: window.location.origin,
      automaticSilentRenew: true,
      silent_redirect_uri: `${window.location.origin}/silent-callback`,
    };

    this.userManager = new UserManager(settings);
  }

  async signIn(): Promise<void> {
    await this.userManager.signinRedirect();
  }

  async signInCallback(): Promise<User | null> {
    return await this.userManager.signinRedirectCallback();
  }

  async signOut(): Promise<void> {
    await this.userManager.signoutRedirect();
  }

  async getUser(): Promise<User | null> {
    return await this.userManager.getUser();
  }

  async isAuthenticated(): Promise<boolean> {
    const user = await this.getUser();
    return user !== null && !user.expired;
  }

  // This method will be enhanced in later tasks to exchange tokens with backend
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  async exchangeTokenWithBackend(_accessToken: string): Promise<AuthResponse> {
    // Placeholder implementation - will be completed in task 15
    throw new Error('Token exchange not implemented yet');
  }

  // Convert OIDC user to our UserInfo type
  mapUserToUserInfo(user: User): UserInfo {
    return {
      userId: user.profile.sub || '',
      email: user.profile.email || '',
      name: user.profile.name || '',
      roles: (user.profile.roles as string[]) || [],
    };
  }
}

export const authService = new AuthService();