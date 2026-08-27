import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { dashboardPathForRole } from "../../utils/routes";

export function UnauthorizedPage() {
  const { auth } = useAuth();
  const dashboardPath = auth ? dashboardPathForRole(auth.role) : "/login";

  return (
    <section className="empty-state">
      <h1>Unauthorized</h1>
      <p>You do not have permission to open this page.</p>
      <Link className="button button-primary" to={dashboardPath}>
        Go back
      </Link>
    </section>
  );
}
