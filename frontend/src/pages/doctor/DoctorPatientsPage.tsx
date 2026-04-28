import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { appointmentApi } from "../../api/appointmentApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { StatusBadge } from "../../components/StatusBadge";
import type { Appointment } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDateTime } from "../../utils/dateTime";

interface PatientSummary {
  patientId: number;
  patientName: string;
  appointmentCount: number;
  scheduledCount: number;
  completedCount: number;
  latestAppointment: Appointment;
  nextAppointment: Appointment | null;
  statuses: Array<Appointment["status"]>;
}

const statusOrder: Array<Appointment["status"]> = ["SCHEDULED", "COMPLETED", "CANCELLED", "NO_SHOW"];

function toTime(value: string): number {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 0 : date.getTime();
}

function buildPatientSummaries(appointments: Appointment[]): PatientSummary[] {
  const grouped = new Map<number, Appointment[]>();

  appointments.forEach((appointment) => {
    const existing = grouped.get(appointment.patientId) || [];
    grouped.set(appointment.patientId, existing.concat(appointment));
  });

  return Array.from(grouped.entries())
    .map(([patientId, patientAppointments]) => {
      const newestFirst = patientAppointments.slice().sort((first, second) => toTime(second.appointmentDateTime) - toTime(first.appointmentDateTime));
      const scheduledAppointments = patientAppointments
        .filter((appointment) => appointment.status === "SCHEDULED")
        .sort((first, second) => toTime(first.appointmentDateTime) - toTime(second.appointmentDateTime));

      return {
        patientId,
        patientName: patientAppointments[0].patientName,
        appointmentCount: patientAppointments.length,
        scheduledCount: scheduledAppointments.length,
        completedCount: patientAppointments.filter((appointment) => appointment.status === "COMPLETED").length,
        latestAppointment: newestFirst[0],
        nextAppointment: scheduledAppointments[0] || null,
        statuses: statusOrder.filter((status) => patientAppointments.some((appointment) => appointment.status === status))
      };
    })
    .sort((first, second) => first.patientName.localeCompare(second.patientName));
}

export function DoctorPatientsPage() {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadPatients() {
      try {
        setLoading(true);
        setError("");
        const page = await appointmentApi.listAppointments({ status: "", page: 0, size: 100 });
        setAppointments(page.content);
      } catch (apiError) {
        setError(getApiErrorMessage(apiError));
      } finally {
        setLoading(false);
      }
    }

    loadPatients();
  }, []);

  const patientSummaries = useMemo(() => buildPatientSummaries(appointments), [appointments]);

  const filteredSummaries = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    if (!normalizedSearch) {
      return patientSummaries;
    }

    return patientSummaries.filter((summary) => {
      return (
        summary.patientName.toLowerCase().includes(normalizedSearch) ||
        summary.latestAppointment.reason.toLowerCase().includes(normalizedSearch)
      );
    });
  }, [patientSummaries, search]);

  const scheduledCount = appointments.filter((appointment) => appointment.status === "SCHEDULED").length;
  const completedCount = appointments.filter((appointment) => appointment.status === "COMPLETED").length;
  const connectedPatientCount = patientSummaries.length;

  return (
    <section className="content-stack">
      <div className="hero-panel hero-doctor">
        <div className="section-heading">
          <p className="eyebrow">Patient records</p>
          <h1>Patients connected to your appointments</h1>
          <p>Open patient records only when MedFlow has an appointment relationship for your account.</p>
        </div>

        <div className="hero-summary-card">
          <span className="summary-label">Record access</span>
          {loading ? (
            <strong>Checking assignments</strong>
          ) : (
            <>
              <strong>{connectedPatientCount} connected patients</strong>
              <span>{scheduledCount} scheduled visits need attention.</span>
            </>
          )}
        </div>
      </div>

      <div className="insight-grid">
        <article className="metric-card">
          <span>Patients</span>
          <strong>{connectedPatientCount}</strong>
          <small>Visible through assigned appointments</small>
        </article>
        <article className="metric-card">
          <span>Scheduled</span>
          <strong>{scheduledCount}</strong>
          <small>Upcoming visits on your list</small>
        </article>
        <article className="metric-card">
          <span>Completed</span>
          <strong>{completedCount}</strong>
          <small>Appointments with visit history</small>
        </article>
      </div>

      <div className="toolbar">
        <label className="field compact-field">
          <span>Search patient</span>
          <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Patient name or reason" />
        </label>
      </div>

      <ErrorMessage message={error} />
      {loading ? <LoadingSpinner label="Loading patient records" /> : null}

      {!loading && patientSummaries.length === 0 ? <div className="empty-state">No patient records found for assigned appointments.</div> : null}

      {!loading && patientSummaries.length > 0 && filteredSummaries.length === 0 ? (
        <div className="empty-state">No matching patient records found.</div>
      ) : null}

      {!loading && filteredSummaries.length > 0 ? (
        <div className="cards-grid patient-record-grid">
          {filteredSummaries.map((summary) => (
            <article key={summary.patientId} className="record-card patient-record-card">
              <div>
                <h2>{summary.patientName}</h2>
                <div className="record-meta-grid">
                  <p>{summary.appointmentCount} total appointments</p>
                  <p>{summary.completedCount} completed visits</p>
                  <p>{summary.scheduledCount} scheduled visits</p>
                </div>

                {summary.nextAppointment ? (
                  <p>Next: {formatDateTime(summary.nextAppointment.appointmentDateTime)}</p>
                ) : (
                  <p>Latest: {formatDateTime(summary.latestAppointment.appointmentDateTime)}</p>
                )}

                <div className="badge-row">
                  {summary.statuses.map((status) => (
                    <StatusBadge key={status} status={status} />
                  ))}
                </div>
              </div>

              <Link className="button button-primary" to={"/doctor/patients/" + summary.patientId}>
                Open record
              </Link>
            </article>
          ))}
        </div>
      ) : null}
    </section>
  );
}
