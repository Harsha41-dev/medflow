import { type FormEvent, useState } from "react";
import { adminApi } from "../../api/adminApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import type { CreateDoctorRequest, Doctor } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";

const initialForm: CreateDoctorRequest = {
  email: "",
  password: "",
  firstName: "",
  lastName: "",
  specialization: "",
  licenseNumber: ""
};

export function AdminDoctorsPage() {
  const [form, setForm] = useState<CreateDoctorRequest>(initialForm);
  const [createdDoctor, setCreatedDoctor] = useState<Doctor | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  function updateField(field: keyof CreateDoctorRequest, value: string) {
    setForm((current) => Object.assign({}, current, { [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setCreatedDoctor(null);

    if (
      !form.email.trim() ||
      !form.password.trim() ||
      !form.firstName.trim() ||
      !form.lastName.trim() ||
      !form.specialization.trim() ||
      !form.licenseNumber.trim()
    ) {
      setError("All doctor account fields are required.");
      return;
    }

    try {
      setSubmitting(true);
      const doctor = await adminApi.createDoctor({
        email: form.email.trim(),
        password: form.password,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        specialization: form.specialization.trim(),
        licenseNumber: form.licenseNumber.trim()
      });
      setCreatedDoctor(doctor);
      setForm(initialForm);
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Doctor accounts</p>
        <h1>Create Doctor</h1>
        <p>Admin-created accounts automatically receive the DOCTOR role.</p>
      </div>

      <ErrorMessage message={error} />
      {createdDoctor ? (
        <div className="alert alert-success">
          Created Dr. {createdDoctor.firstName} {createdDoctor.lastName} for {createdDoctor.email}.
        </div>
      ) : null}

      <form className="panel form-grid two-columns" onSubmit={handleSubmit}>
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
          <span>Specialization</span>
          <input value={form.specialization} onChange={(event) => updateField("specialization", event.target.value)} required />
        </label>

        <label className="field">
          <span>License number</span>
          <input value={form.licenseNumber} onChange={(event) => updateField("licenseNumber", event.target.value)} required />
        </label>

        <button type="submit" className="button button-primary full-width" disabled={submitting}>
          {submitting ? "Creating doctor" : "Create doctor"}
        </button>
      </form>
    </section>
  );
}
