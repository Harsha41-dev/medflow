import { type FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ErrorMessage } from "../../components/ErrorMessage";
import { useAuth } from "../../context/AuthContext";
import type { LoginRequest } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { dashboardPathForRole } from "../../utils/routes";

const initialForm: LoginRequest = {
  email: "",
  password: ""
};

export function LoginPage() {
  const [form, setForm] = useState<LoginRequest>(initialForm);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const { auth, isAuthenticated, login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated && auth) {
      navigate(dashboardPathForRole(auth.role), { replace: true });
    }
  }, [auth, isAuthenticated, navigate]);

  function updateField(field: keyof LoginRequest, value: string) {
    setForm((current) => Object.assign({}, current, { [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (!form.email.trim() || !form.password.trim()) {
      setError("Email and password are required.");
      return;
    }

    try {
      setSubmitting(true);
      const nextAuth = await login({
        email: form.email.trim(),
        password: form.password
      });
      navigate(dashboardPathForRole(nextAuth.role), { replace: true });
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-panel auth-card">
        <div className="auth-visual">
          <span className="brand-mark auth-brand-mark" aria-hidden="true">M</span>
          <strong>Clinic work, neatly connected.</strong>
          <p>Appointments, encounters, prescriptions, and audit trails in one role-based workspace.</p>
        </div>

        <div className="auth-form-panel">
          <div className="section-heading">
            <p className="eyebrow">Welcome back</p>
            <h1>Login to MedFlow</h1>
            <p>Use your registered email and password to continue.</p>
          </div>

          <ErrorMessage message={error} />

          <form className="form-grid" onSubmit={handleSubmit}>
            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={form.email}
                onChange={(event) => updateField("email", event.target.value)}
                autoComplete="email"
                required
              />
            </label>

            <label className="field">
              <span>Password</span>
              <input
                type="password"
                value={form.password}
                onChange={(event) => updateField("password", event.target.value)}
                autoComplete="current-password"
                required
              />
            </label>

            <button type="submit" className="button button-primary" disabled={submitting}>
              {submitting ? "Logging in" : "Login"}
            </button>
          </form>

          <p className="auth-switch">
            New patient? <Link to="/register">Create a patient account</Link>
          </p>
        </div>
      </div>
    </section>
  );
}
