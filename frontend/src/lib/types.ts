// ── Auth API Types ───────────────────────────────────────────

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface RegisterResponse {
  message: string;
  email: string;
  emailVerified: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  tokenType: string;
  expiresIn: number;
  accessToken: string;
}

export interface UserProfile {
  id: string;
  email: string;
  emailVerified: boolean;
  status: 'ACTIVE' | 'DISABLED' | 'LOCKED';
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface UserSummary {
  id: string;
  email: string;
  emailVerified: boolean;
  status: string;
  roles: string[];
  createdAt: string;
}

export interface ApiError {
  status: number;
  message: string;
  errors?: Record<string, string>;
}

export interface ResetPasswordRequest {
  email: string;
}

export interface ResetPasswordConfirmRequest {
  token: string;
  password: string;
}
