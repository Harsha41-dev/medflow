import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { appointmentApi } from "../../api/appointmentApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { StatusBadge } from "../../components/StatusBadge";
import { useAuth } from "../../context/AuthContext";
import type { Appointment } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDateTime } from "../../utils/dateTime";

export function DoctorDashboardPage() {
  const { auth } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadAppointments() {
      try {
        setLoading(true);
        const page = await appointmentApi.listAppointments({ status: "SCHEDULED", page: 0, size: 5 });
        setAppointments(page.content);
      } catch (apiError) {
        setError(getApiErrorMessage(apiError));
      } finally {
        setLoading(false);
      }
    }

    loadAppointments();
  }, []);

  const nextAppointment = appointments[0];

  return (
    <section className="content-stack">
      <div className="hero-panel hero-doctor">
        <div className="section-heading">
          <p className="eyebrow">Doctor dashboard</p>
          <h1>Clinical workspace</h1>
          <p>{auth?.email}</p>
        </div>

        <div className="hero-summary-card">
          <span className="summary-label">Next patient</span>
          {loading ? (
            <strong>Checking schedule</strong>
          ) : nextAppointment ? (
            <>
              <strong>{nextAppointment.patientName}</strong>
              <span>{formatDateTime(nextAppointment.appointmentDateTime)}</span>
            </>
          ) : (
            <>
              <strong>No assigned visits</strong>
              <span>Scheduled appointments will appear here.</span>
            </>
          )}
        </div>
      </div>

      <div className="quick-grid">
        <Link to="/doctor/appointments" className="quick-card">
          <span>Assigned Appointments</span>
          <small>Start encounters</small>
        </Link>
        <Link to="/doctor/patients" className="quick-card">
          <span>Patient Records</span>
          <small>Review connected patients</small>
        </Link>
        <Link to="/doctor/symptom-classifier" className="quick-card">
          <span>Symptom Pattern Classifier</span>
          <small>Educational ML tool</small>
        </Link>
      </div>

      <div className="panel">
        <div className="panel-header">
          <h2>Scheduled appointments</h2>
          <Link to="/doctor/appointments">View all</Link>
        </div>

        <ErrorMessage message={error} />
        {loading ? <LoadingSpinner label="Loading appointments" /> : null}

        {!loading && appointments.length === 0 ? <div className="empty-inline">No scheduled appointments found.</div> : null}

        {!loading && appointments.length > 0 ? (
          <div className="list-stack">
            {appointments.map((appointment) => (
              <article key={appointment.id} className="record-card">
                <div>
                  <h3>{appointment.patientName}</h3>
                  <p>{formatDateTime(appointment.appointmentDateTime)}</p>
                  <p>{appointment.reason}</p>
                </div>
                <StatusBadge status={appointment.status} />
              </article>
            ))}
          </div>
        ) : null}
      </div>
    </section>
  );
}
