import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { MedFlowGuideBot } from "./MedFlowGuideBot";

interface NavItem {
  label: string;
  path: string;
}

const patientNav: NavItem[] = [
  { label: "Dashboard", path: "/patient/dashboard" },
  { label: "Profile", path: "/patient/profile" },
  { label: "Doctors", path: "/patient/doctors" },
  { label: "Appointments", path: "/patient/appointments" },
  { label: "Timeline", path: "/patient/timeline" },
  { label: "Medical History", path: "/patient/medical-history" },
  { label: "Prescriptions", path: "/patient/prescriptions" }
];

const doctorNav: NavItem[] = [
  { label: "Dashboard", path: "/doctor/dashboard" },
  { label: "Appointments", path: "/doctor/appointments" },
  { label: "Patients", path: "/doctor/patients" },
  { label: "Symptom Classifier", path: "/doctor/symptom-classifier" }
];

const adminNav: NavItem[] = [
  { label: "Dashboard", path: "/admin/dashboard" },
  { label: "Doctors", path: "/admin/doctors" },
  { label: "Audit Logs", path: "/admin/audit-logs" }
];

export function Layout() {
  const { auth, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const shellClassName = auth
    ? "app-shell app-shell-authenticated app-shell-" + auth.role.toLowerCase()
    : "app-shell app-shell-public";

  function navItems(): NavItem[] {
    if (!auth) {
      return [];
    }

    if (auth.role === "PATIENT") {
      return patientNav;
    }

    if (auth.role === "DOCTOR") {
      return doctorNav;
    }

    return adminNav;
  }

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <div className={shellClassName}>
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand" aria-label="MedFlow home">
            <span className="brand-mark" aria-hidden="true">M</span>
            <span className="brand-copy">
              <strong>MedFlow</strong>
              <span>Clinic workspace</span>
            </span>
          </NavLink>

          <nav className="nav-links" aria-label="Main navigation">
            {isAuthenticated
              ? navItems().map((item) => (
                  <NavLink key={item.path} to={item.path} className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}>
                    {item.label}
                  </NavLink>
                ))
              : (
                  <>
                    <NavLink to="/login" className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}>
                      Login
                    </NavLink>
                    <NavLink to="/register" className={({ isActive }) => (isActive ? "nav-link active" : "nav-link")}>
                      Register
                    </NavLink>
                  </>
                )}
          </nav>

          {isAuthenticated && auth ? (
            <div className="user-chip">
              <span className="user-email">{auth.email}</span>
              <span className="role-pill">{auth.role}</span>
              <button type="button" className="button button-secondary button-compact" onClick={handleLogout}>
                Logout
              </button>
            </div>
          ) : null}
        </div>
      </header>

      <main className="page-container">
        <Outlet />
      </main>

      <MedFlowGuideBot />
    </div>
  );
}
