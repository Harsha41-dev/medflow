import { type FormEvent, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ErrorMessage } from "../../components/ErrorMessage";
import { useAuth } from "../../context/AuthContext";
import type { Gender, RegisterRequest } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { dashboardPathForRole } from "../../utils/routes";

const initialForm: RegisterRequest = {
  email: "",
  password: "",
  firstName: "",
  lastName: "",
  dateOfBirth: "",
  gender: "MALE",
  phone: "",
  address: "",
  emergencyContact: ""
};

export function RegisterPage() {
  const [form, setForm] = useState<RegisterRequest>(initialForm);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const { auth, isAuthenticated, register } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated && auth) {
      navigate(dashboardPathForRole(auth.role), { replace: true });
    }
  }, [auth, isAuthenticated, navigate]);

  function updateField(field: keyof RegisterRequest, value: string) {
    setForm((current) => Object.assign({}, current, { [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (!form.email.trim() || !form.password.trim() || !form.firstName.trim() || !form.lastName.trim()) {
      setError("Email, password, first name, and last name are required.");
      return;
    }

    try {
      setSubmitting(true);
      const nextAuth = await register({
        email: form.email.trim(),
        password: form.password,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        dateOfBirth: form.dateOfBirth,
        gender: form.gender,
        phone: form.phone.trim(),
        address: form.address.trim(),
        emergencyContact: form.emergencyContact.trim()
      });
      navigate(dashboardPathForRole(nextAuth.role), { replace: true });
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="auth-page wide">
      <div className="auth-panel auth-card">
        <div className="auth-visual">
          <span className="brand-mark auth-brand-mark" aria-hidden="true">M</span>
          <strong>Start as a patient.</strong>
          <p>Doctor access is created separately by an admin, so public signup stays role-safe.</p>
        </div>

        <div className="auth-form-panel">
          <div className="section-heading">
            <p className="eyebrow">Patient registration</p>
            <h1>Create your MedFlow account</h1>
            <p>Public registration creates a patient account only.</p>
          </div>

          <ErrorMessage message={error} />

          <form className="form-grid two-columns" onSubmit={handleSubmit}>
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
                minLength={8}
                value={form.password}
                onChange={(event) => updateField("password", event.target.value)}
                autoComplete="new-password"
                required
              />
            </label>

            <label className="field">
              <span>First name</span>
              <input value={form.firstName} onChange={(event) => updateField("firstName", event.target.value)} required />
            </label>

            <label className="field">
              <span>Last name</span>
              <input value={form.lastName} onChange={(event) => updateField("lastName", event.target.value)} required />
            </label>

            <label className="field">
              <span>Date of birth</span>
              <input
                type="date"
                value={form.dateOfBirth}
                onChange={(event) => updateField("dateOfBirth", event.target.value)}
                required
              />
            </label>

            <label className="field">
              <span>Gender</span>
              <select value={form.gender} onChange={(event) => updateField("gender", event.target.value as Gender)} required>
                <option value="MALE">MALE</option>
                <option value="FEMALE">FEMALE</option>
                <option value="OTHER">OTHER</option>
              </select>
            </label>

            <label className="field">
              <span>Phone</span>
              <input value={form.phone} onChange={(event) => updateField("phone", event.target.value)} required />
            </label>

            <label className="field">
              <span>Emergency contact</span>
              <input
                value={form.emergencyContact}
                onChange={(event) => updateField("emergencyContact", event.target.value)}
                required
              />
            </label>

            <label className="field full-width">
              <span>Address</span>
              <textarea value={form.address} onChange={(event) => updateField("address", event.target.value)} rows={3} required />
            </label>

            <button type="submit" className="button button-primary full-width" disabled={submitting}>
              {submitting ? "Creating account" : "Register as patient"}
            </button>
          </form>

          <p className="auth-switch">
            Already registered? <Link to="/login">Login</Link>
          </p>
        </div>
      </div>
    </section>
  );
}
