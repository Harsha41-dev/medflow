import { useEffect, useState } from "react";
import { appointmentApi } from "../../api/appointmentApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { Pagination } from "../../components/Pagination";
import { StatusBadge } from "../../components/StatusBadge";
import type { Appointment, AppointmentStatus, PageResponse } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDateTime } from "../../utils/dateTime";

export function PatientAppointmentsPage() {
  const [appointmentsPage, setAppointmentsPage] = useState<PageResponse<Appointment> | null>(null);
  const [status, setStatus] = useState<AppointmentStatus | "">("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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

  async function cancelAppointment(appointmentId: number) {
    try {
      setError("");
      await appointmentApi.cancelAppointment(appointmentId);
      await loadAppointments();
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    }
  }

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Appointments</p>
        <h1>My Appointments</h1>
        <p>Cancelled appointments remain visible for history.</p>
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

      {!loading && appointmentsPage?.content.length === 0 ? <div className="empty-state">No appointments found.</div> : null}

      {!loading && appointmentsPage && appointmentsPage.content.length > 0 ? (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Doctor</th>
                  <th>Specialization</th>
                  <th>Date and time</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {appointmentsPage.content.map((appointment) => (
                  <tr key={appointment.id}>
                    <td>{appointment.doctorName}</td>
                    <td>{appointment.doctorSpecialization}</td>
                    <td>{formatDateTime(appointment.appointmentDateTime)}</td>
                    <td>{appointment.reason}</td>
                    <td>
                      <StatusBadge status={appointment.status} />
                    </td>
                    <td>
                      {appointment.status === "SCHEDULED" ? (
                        <button type="button" className="button button-danger button-compact" onClick={() => cancelAppointment(appointment.id)}>
                          Cancel
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
    </section>
  );
}
