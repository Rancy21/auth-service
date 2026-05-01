import { browser } from '$app/environment';
import type {
  RegisterRequest,
  RegisterResponse,
  LoginRequest,
  LoginResponse,
  UserProfile,
  UserSummary,
  ResetPasswordRequest,
  ResetPasswordConfirmRequest,
} from './types';

const BASE = 'https://localhost:8443';

// ── Shared fetch wrapper ─────────────────────────────────────

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    credentials: 'include', // always send cookies
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    const message =
      body.message || body.error || `Request failed (${res.status})`;
    const error = new Error(message) as Error & { status: number };
    error.status = res.status;
    throw error;
  }

  return res.json();
}

// ── Auth helpers ─────────────────────────────────────────────

function authHeaders(): Record<string, string> {
  if (!browser) return {};
  const token = localStorage.getItem('accessToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// ── Public endpoints ─────────────────────────────────────────

export function register(data: RegisterRequest): Promise<RegisterResponse> {
  return request('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function verifyEmail(token: string): Promise<{ message: string }> {
  return request('/api/v1/auth/verify', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export function resendVerification(email: string): Promise<{ message: string }> {
  return request('/api/v1/auth/resend-verification', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export function requestPasswordReset(
  data: ResetPasswordRequest,
): Promise<{ message: string }> {
  return request('/api/v1/auth/password-reset/request', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function confirmPasswordReset(
  data: ResetPasswordConfirmRequest,
): Promise<{ message: string }> {
  return request('/api/v1/auth/password-reset/confirm', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

// ── Authenticated endpoints ──────────────────────────────────

export function refreshToken(): Promise<LoginResponse> {
  return request<LoginResponse>('/api/v1/auth/refresh', {
    method: 'POST',
  });
}

export function logout(): Promise<{ message: string }> {
  return request('/api/v1/auth/logout', {
    method: 'POST',
  });
}

export function getProfile(): Promise<UserProfile> {
  return request<UserProfile>('/api/v1/me', {
    headers: authHeaders(),
  });
}

export function listUsers(): Promise<UserSummary[]> {
  return request<UserSummary[]>('/api/v1/admin/users', {
    headers: authHeaders(),
  });
}
