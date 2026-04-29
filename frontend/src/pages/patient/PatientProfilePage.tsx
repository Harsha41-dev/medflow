import { type FormEvent, useEffect, useState } from "react";
import { patientApi } from "../../api/patientApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { useAuth } from "../../context/AuthContext";
import type { Patient, PatientUpdateRequest } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";

const emptyForm: PatientUpdateRequest = {
  firstName: "",
  lastName: "",
  phone: "",
  address: "",
  emergencyContact: ""
};

export function PatientProfilePage() {
  const { auth } = useAuth();
  const [patient, setPatient] = useState<Patient | null>(null);
  const [form, setForm] = useState<PatientUpdateRequest>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    async function loadProfile() {
      if (!auth?.profileId) {
        setError("Patient profile was not found for this account.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const data = await patientApi.getPatient(auth.profileId);
        setPatient(data);
        setForm({
          firstName: data.firstName,
          lastName: data.lastName,
          phone: data.phone,
          address: data.address,
          emergencyContact: data.emergencyContact
        });
      } catch (apiError) {
        setError(getApiErrorMessage(apiError));
      } finally {
        setLoading(false);
      }
    }

    loadProfile();
  }, [auth?.profileId]);

  function updateField(field: keyof PatientUpdateRequest, value: string) {
    setForm((current) => Object.assign({}, current, { [field]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auth?.profileId) {
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccess("");
      const updated = await patientApi.updatePatient(auth.profileId, form);
      setPatient(updated);
      setSuccess("Profile updated successfully.");
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <LoadingSpinner label="Loading profile" />;
  }

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Patient profile</p>
        <h1>My Profile</h1>
        <p>Update your contact details while keeping medical identity fields unchanged.</p>
      </div>

      <ErrorMessage message={error} />
      {success ? <div className="alert alert-success">{success}</div> : null}

      {patient ? (
        <div className="two-panel-layout">
          <div className="panel">
            <h2>Profile details</h2>
            <dl className="detail-list">
              <div>
                <dt>Date of birth</dt>
                <dd>{patient.dateOfBirth}</dd>
              </div>
              <div>
                <dt>Gender</dt>
                <dd>{patient.gender}</dd>
              </div>
            </dl>
          </div>

          <form className="panel form-grid" onSubmit={handleSubmit}>
            <h2>Edit contact details</h2>
            <label className="field">
              <span>First name</span>
              <input value={form.firstName} onChange={(event) => updateField("firstName", event.target.value)} required />
            </label>
            <label className="field">
              <span>Last name</span>
              <input value={form.lastName} onChange={(event) => updateField("lastName", event.target.value)} required />
            </label>
            <label className="field">
              <span>Phone</span>
              <input value={form.phone} onChange={(event) => updateField("phone", event.target.value)} required />
            </label>
            <label className="field">
              <span>Emergency contact</span>
              <input value={form.emergencyContact} onChange={(event) => updateField("emergencyContact", event.target.value)} required />
            </label>
            <label className="field">
              <span>Address</span>
              <textarea value={form.address} onChange={(event) => updateField("address", event.target.value)} rows={3} required />
            </label>
            <button type="submit" className="button button-primary" disabled={saving}>
              {saving ? "Saving" : "Save profile"}
            </button>
          </form>
        </div>
      ) : null}
    </section>
  );
}
