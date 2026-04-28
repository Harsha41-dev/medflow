import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { authApi } from "../api/authApi";
import type { AuthState, LoginRequest, RegisterRequest } from "../types";
import { AUTH_CHANGED_EVENT, clearStoredAuth, getStoredAuth, saveStoredAuth, toAuthState } from "../utils/authStorage";

interface AuthContextValue {
  auth: AuthState | null;
  isAuthenticated: boolean;
  login: (request: LoginRequest) => Promise<AuthState>;
  register: (request: RegisterRequest) => Promise<AuthState>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(() => getStoredAuth());

  useEffect(() => {
    function syncAuth() {
      setAuth(getStoredAuth());
    }

    window.addEventListener(AUTH_CHANGED_EVENT, syncAuth);
    return () => window.removeEventListener(AUTH_CHANGED_EVENT, syncAuth);
  }, []);

  async function login(request: LoginRequest): Promise<AuthState> {
    const response = await authApi.login(request);
    const nextAuth = toAuthState(response);
    saveStoredAuth(nextAuth);
    setAuth(nextAuth);
    return nextAuth;
  }

  async function register(request: RegisterRequest): Promise<AuthState> {
    const response = await authApi.register(request);
    const nextAuth = toAuthState(response);
    saveStoredAuth(nextAuth);
    setAuth(nextAuth);
    return nextAuth;
  }

  function logout(): void {
    clearStoredAuth();
    setAuth(null);
  }

  const value = useMemo<AuthContextValue>(() => {
    return {
      auth,
      isAuthenticated: auth?.isAuthenticated === true,
      login,
      register,
      logout
    };
  }, [auth]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
