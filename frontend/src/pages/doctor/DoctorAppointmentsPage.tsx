import { type FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { appointmentApi } from "../../api/appointmentApi";
import { clinicalApi } from "../../api/clinicalApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { Pagination } from "../../components/Pagination";
import { StatusBadge } from "../../components/StatusBadge";
import type { Appointment, AppointmentStatus, EncounterRequest, PageResponse } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDateTime } from "../../utils/dateTime";

const emptyEncounterForm: EncounterRequest = {
  appointmentId: 0,
  chiefComplaint: "",
  notes: ""
};

export function DoctorAppointmentsPage() {
  const [appointmentsPage, setAppointmentsPage] = useState<PageResponse<Appointment> | null>(null);
  const [status, setStatus] = useState<AppointmentStatus | "">("");
  const [page, setPage] = useState(0);
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);
  const [encounterForm, setEncounterForm] = useState<EncounterRequest>(emptyEncounterForm);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    loadAppointments();
  }, [page, status]);

  async function loadAppointments() {
    try {
      setLoading(true);
      setError("");
      const data = await appointmentApi.listAppointments({ status, page, size: 10 });
      setAppointmentsPage(data);
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setLoading(false);
    }
  }

  function startEncounter(appointment: Appointment) {
    setSelectedAppointment(appointment);
    setEncounterForm({
      appointmentId: appointment.id,
      chiefComplaint: "",
      notes: ""
    });
  }

  function updateEncounterField(field: keyof EncounterRequest, value: string) {
    setEncounterForm((current) => Object.assign({}, current, { [field]: value }));
  }

  async function submitEncounter(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!selectedAppointment) {
      return;
    }

    if (!encounterForm.chiefComplaint.trim()) {
      setError("Chief complaint is required.");
      return;
    }

    try {
      setSubmitting(true);
      setError("");
      const created = await clinicalApi.createEncounter({
        appointmentId: selectedAppointment.id,
        chiefComplaint: encounterForm.chiefComplaint.trim(),
        notes: encounterForm.notes.trim()
      });
      navigate("/doctor/encounters/" + created.id);
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Doctor appointments</p>
        <h1>Assigned Appointments</h1>
        <p>Create encounters from scheduled appointments assigned to you.</p>
      </div>

      <div className="toolbar">
        <label className="field compact-field">
          <span>Status</span>
          <select value={status} onChange={(event) => { setPage(0); setStatus(event.target.value as AppointmentStatus | ""); }}>
            <option value="">All</option>
            <option value="SCHEDULED">SCHEDULED</option>
            <option value="COMPLETED">COMPLETED</option>
            <option value="CANCELLED">CANCELLED</option>
            <option value="NO_SHOW">NO_SHOW</option>
          </select>
        </label>
      </div>

      <ErrorMessage message={error} />
      {loading ? <LoadingSpinner label="Loading appointments" /> : null}

      {!loading && appointmentsPage?.content.length === 0 ? <div className="empty-state">No assigned appointments found.</div> : null}

      {!loading && appointmentsPage && appointmentsPage.content.length > 0 ? (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Patient</th>
                  <th>Date and time</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {appointmentsPage.content.map((appointment) => (
                  <tr key={appointment.id}>
                    <td>{appointment.patientName}</td>
                    <td>{formatDateTime(appointment.appointmentDateTime)}</td>
                    <td>{appointment.reason}</td>
                    <td>
                      <StatusBadge status={appointment.status} />
                    </td>
                    <td>
                      {appointment.status === "SCHEDULED" ? (
                        <button type="button" className="button button-primary button-compact" onClick={() => startEncounter(appointment)}>
                          Create Encounter
                        </button>
                      ) : (
                        <span className="muted-text">No action</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination page={page} totalPages={appointmentsPage.totalPages} onPageChange={setPage} />
        </>
      ) : null}

      {selectedAppointment ? (
        <form className="panel form-grid" onSubmit={submitEncounter}>
          <h2>Create encounter for {selectedAppointment.patientName}</h2>
          <label className="field">
            <span>Chief complaint</span>
            <input
              value={encounterForm.chiefComplaint}
              onChange={(event) => updateEncounterField("chiefComplaint", event.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>Notes</span>
            <textarea value={encounterForm.notes} onChange={(event) => updateEncounterField("notes", event.target.value)} rows={4} />
          </label>
          <div className="button-row">
            <button type="submit" className="button button-primary" disabled={submitting}>
              {submitting ? "Creating encounter" : "Create encounter"}
            </button>
            <button type="button" className="button button-secondary" onClick={() => setSelectedAppointment(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}
    </section>
  );
}
