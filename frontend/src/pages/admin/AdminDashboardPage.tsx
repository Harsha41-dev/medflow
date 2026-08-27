import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export function AdminDashboardPage() {
  const { auth } = useAuth();

  return (
    <section className="content-stack">
      <div className="hero-panel hero-admin">
        <div className="section-heading">
          <p className="eyebrow">Admin dashboard</p>
          <h1>Operations console</h1>
          <p>{auth?.email}</p>
        </div>

        <div className="hero-summary-card">
          <span className="summary-label">Admin scope</span>
          <strong>Doctors and audit logs</strong>
          <span>Review access-sensitive system activity.</span>
        </div>
      </div>

      <div className="quick-grid">
        <Link to="/admin/doctors" className="quick-card">
          <span>Create Doctor</span>
          <small>Provision clinical users</small>
        </Link>
        <Link to="/admin/audit-logs" className="quick-card">
          <span>Audit Logs</span>
          <small>Track important actions</small>
        </Link>
      </div>
    </section>
  );
}
