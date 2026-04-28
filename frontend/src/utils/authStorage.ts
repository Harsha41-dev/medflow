import type { AuthResponse, AuthState } from "../types";

const AUTH_STORAGE_KEY = "medflowAuth";
const GUIDE_STORAGE_PREFIX = "medflowGuideMessages:";
const GUIDE_INTRO_STORAGE_PREFIX = "medflowGuideIntroDismissed:";

export const AUTH_CHANGED_EVENT = "medflow-auth-changed";

export function toAuthState(response: AuthResponse): AuthState {
  return {
    accessToken: response.accessToken,
    tokenType: response.tokenType,
    userId: response.userId,
    profileId: response.profileId,
    email: response.email,
    role: response.role,
    isAuthenticated: true
  };
}

export function getStoredAuth(): AuthState | null {
  const stored = window.localStorage.getItem(AUTH_STORAGE_KEY);
  if (!stored) {
    return null;
  }

  try {
    return JSON.parse(stored) as AuthState;
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function saveStoredAuth(auth: AuthState): void {
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}

export function clearStoredAuth(): void {
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
  Object.keys(window.localStorage)
    .filter((key) => key.startsWith(GUIDE_STORAGE_PREFIX) || key.startsWith(GUIDE_INTRO_STORAGE_PREFIX))
    .forEach((key) => window.localStorage.removeItem(key));
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}
