import { browser } from '$app/environment';
import * as api from '$lib/api';
import type { UserProfile } from '$lib/types';

// ── Reactive auth store (Svelte 5 runes) ─────────────────────

// Shared module-level state – every component importing this file
// sees the same reactive values.
let user = $state<UserProfile | null>(null);
let loading = $state(true);
let error = $state<string | null>(null);

// Derived values
const isAuthenticated = $derived(user !== null);
const isAdmin = $derived(user?.roles.includes('ADMIN') ?? false);

// ── Bootstrap ────────────────────────────────────────────────

async function init(): Promise<void> {
  if (!browser) {
    loading = false;
    return;
  }

  const token = localStorage.getItem('accessToken');
  if (!token) {
    loading = false;
    return;
  }

  // Try fetching profile with existing token
  try {
    user = await api.getProfile();
  } catch (e: unknown) {
    const err = e as { status?: number };
    // If unauthorized, try refreshing the token
    if (err.status === 401) {
      try {
        const refreshed = await api.refreshToken();
        localStorage.setItem('accessToken', refreshed.accessToken);
        user = await api.getProfile();
      } catch {
        // Refresh failed – clear everything
        localStorage.removeItem('accessToken');
      }
    }
  } finally {
    loading = false;
  }
}

// ── Actions ──────────────────────────────────────────────────

async function loginAction(email: string, password: string): Promise<void> {
  error = null;
  const res = await api.login({ email, password });
  localStorage.setItem('accessToken', res.accessToken);
  user = await api.getProfile();
}

async function registerAction(email: string, password: string): Promise<void> {
  error = null;
  await api.register({ email, password });
}

async function logoutAction(): Promise<void> {
  try {
    await api.logout();
  } finally {
    localStorage.removeItem('accessToken');
    user = null;
    error = null;
  }
}

async function refreshAction(): Promise<boolean> {
  try {
    const res = await api.refreshToken();
    localStorage.setItem('accessToken', res.accessToken);
    user = await api.getProfile();
    return true;
  } catch {
    localStorage.removeItem('accessToken');
    user = null;
    return false;
  }
}

export const auth = {
  // Reactive state (read-only from outside via getters)
  get user() { return user; },
  get loading() { return loading; },
  get error() { return error; },
  get isAuthenticated() { return isAuthenticated; },
  get isAdmin() { return isAdmin; },

  // Functions
  init,
  login: loginAction,
  register: registerAction,
  logout: logoutAction,
  refresh: refreshAction,
};
