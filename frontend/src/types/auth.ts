export interface UserInfo {
  userId: string;
  email: string;
  name: string;
  roles: string[];
}

export interface AuthState {
  isAuthenticated: boolean;
  user: UserInfo | null;
  sessionToken: string | null;
  accessToken: string | null;
  loading: boolean;
  error: string | null;
}

export interface AuthResponse {
  sessionToken: string;
  user: UserInfo;
  expiresIn: number;
}

export interface OidcConfig {
  authority: string;
  clientId: string;
  redirectUri: string;
  scope: string;
  responseType: string;
  usePkce: boolean;
}