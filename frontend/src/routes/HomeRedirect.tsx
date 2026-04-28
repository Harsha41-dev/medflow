import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { dashboardPathForRole } from "../utils/routes";

export function HomeRedirect() {
  const { auth, isAuthenticated } = useAuth();

  if (!isAuthenticated || !auth) {
    return <Navigate to="/login" replace />;
  }

  return <Navigate to={dashboardPathForRole(auth.role)} replace />;
}
