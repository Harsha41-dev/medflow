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

export function PatientDashboardPage() {
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
      <div className="hero-panel hero-patient">
        <div className="section-heading">
          <p className="eyebrow">Patient dashboard</p>
          <h1>Welcome back</h1>
          <p>{auth?.email}</p>
        </div>

        <div className="hero-summary-card">
          <span className="summary-label">Next appointment</span>
          {loading ? (
            <strong>Checking schedule</strong>
          ) : nextAppointment ? (
            <>
              <strong>{nextAppointment.doctorName}</strong>
              <span>{formatDateTime(nextAppointment.appointmentDateTime)}</span>
            </>
          ) : (
            <>
              <strong>No visit scheduled</strong>
              <span>Book a slot with an available doctor.</span>
            </>
          )}
        </div>
      </div>

      <div className="quick-grid">
        <Link to="/patient/book-appointment" className="quick-card">
          <span>Book Appointment</span>
          <small>Choose doctor and time</small>
        </Link>
        <Link to="/patient/appointments" className="quick-card">
          <span>My Appointments</span>
          <small>Scheduled and past visits</small>
        </Link>
        <Link to="/patient/timeline" className="quick-card">
          <span>Care Timeline</span>
          <small>Visits and prescriptions together</small>
        </Link>
        <Link to="/patient/medical-history" className="quick-card">
          <span>Medical History</span>
          <small>Completed encounters</small>
        </Link>
        <Link to="/patient/prescriptions" className="quick-card">
          <span>Prescriptions</span>
          <small>Medication records</small>
        </Link>
        <Link to="/patient/profile" className="quick-card">
          <span>Profile</span>
          <small>Contact details</small>
        </Link>
      </div>

      <div className="panel">
        <div className="panel-header">
          <h2>Upcoming appointments</h2>
          <Link to="/patient/appointments">View all</Link>
        </div>

        <ErrorMessage message={error} />
        {loading ? <LoadingSpinner label="Loading appointments" /> : null}

        {!loading && appointments.length === 0 ? (
          <div className="empty-inline">No upcoming appointments found.</div>
        ) : null}

        {!loading && appointments.length > 0 ? (
          <div className="list-stack">
            {appointments.map((appointment) => (
              <article key={appointment.id} className="record-card">
                <div>
                  <h3>{appointment.doctorName}</h3>
                  <p>{appointment.doctorSpecialization}</p>
                  <p>{formatDateTime(appointment.appointmentDateTime)}</p>
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
