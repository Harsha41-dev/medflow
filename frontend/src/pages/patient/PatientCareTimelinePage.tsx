import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { appointmentApi } from "../../api/appointmentApi";
import { clinicalApi } from "../../api/clinicalApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { StatusBadge } from "../../components/StatusBadge";
import { useAuth } from "../../context/AuthContext";
import type { Appointment, Encounter, Prescription } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDateTime } from "../../utils/dateTime";

interface TimelineItem {
  id: string;
  label: string;
  title: string;
  dateTime: string;
  subtitle: string;
  description: string;
  status?: Appointment["status"];
}

function toTime(value: string): number {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 0 : date.getTime();
}

function formatTimelineDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Date";
  }

  return date.toLocaleDateString(undefined, { day: "2-digit", month: "short" });
}

function formatTimelineTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Time";
  }

  return date.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
}

export function PatientCareTimelinePage() {
  const { auth } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [encounters, setEncounters] = useState<Encounter[]>([]);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadTimeline() {
      if (!auth?.profileId) {
        setError("Patient profile was not found for this account.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError("");
        const [appointmentPage, encounterPage, prescriptionPage] = await Promise.all([
          appointmentApi.listAppointments({ status: "", page: 0, size: 50 }),
          clinicalApi.listPatientEncounters(auth.profileId, 0, 20),
          clinicalApi.listPatientPrescriptions(auth.profileId, 0, 20)
        ]);

        setAppointments(appointmentPage.content);
        setEncounters(encounterPage.content);
        setPrescriptions(prescriptionPage.content);
      } catch (apiError) {
        setError(getApiErrorMessage(apiError));
      } finally {
        setLoading(false);
      }
    }

    loadTimeline();
  }, [auth?.profileId]);

  const nextAppointment = useMemo(() => {
    return appointments
      .filter((appointment) => appointment.status === "SCHEDULED")
      .slice()
      .sort((first, second) => toTime(first.appointmentDateTime) - toTime(second.appointmentDateTime))[0];
  }, [appointments]);

  const timelineItems = useMemo<TimelineItem[]>(() => {
    const appointmentItems: TimelineItem[] = appointments.map((appointment) => ({
      id: "appointment-" + appointment.id,
      label: "Appointment",
      title: appointment.doctorName,
      dateTime: appointment.appointmentDateTime,
      subtitle: appointment.doctorSpecialization,
      description: appointment.reason,
      status: appointment.status
    }));

    const encounterItems: TimelineItem[] = encounters.map((encounter) => ({
      id: "encounter-" + encounter.id,
      label: "Encounter",
      title: encounter.chiefComplaint,
      dateTime: encounter.visitDate,
      subtitle: "Doctor: " + encounter.doctorName,
      description: encounter.notes || "Encounter note recorded for this visit."
    }));

    return appointmentItems.concat(encounterItems).sort((first, second) => toTime(second.dateTime) - toTime(first.dateTime));
  }, [appointments, encounters]);

  const scheduledCount = appointments.filter((appointment) => appointment.status === "SCHEDULED").length;
  const completedCount = appointments.filter((appointment) => appointment.status === "COMPLETED").length;

  return (
    <section className="content-stack">
      <div className="hero-panel hero-patient">
        <div className="section-heading">
          <p className="eyebrow">Care timeline</p>
          <h1>Care Timeline</h1>
          <p>Appointments, encounter notes, and prescriptions connected to your clinic visits.</p>
        </div>

        <div className="hero-summary-card">
          <span className="summary-label">Next visit</span>
          {loading ? (
            <strong>Checking your timeline</strong>
          ) : nextAppointment ? (
            <>
              <strong>{nextAppointment.doctorName}</strong>
              <span>{formatDateTime(nextAppointment.appointmentDateTime)}</span>
            </>
          ) : (
            <>
              <strong>No scheduled visit</strong>
              <span>Book a doctor from the appointment screen.</span>
            </>
          )}
        </div>
      </div>

      <div className="insight-grid">
        <article className="metric-card">
          <span>Scheduled</span>
          <strong>{scheduledCount}</strong>
          <small>Upcoming appointment slots</small>
        </article>
        <article className="metric-card">
          <span>Completed</span>
          <strong>{completedCount}</strong>
          <small>Appointments converted to visits</small>
        </article>
        <article className="metric-card">
          <span>Encounters</span>
          <strong>{encounters.length}</strong>
          <small>Clinical notes shared with you</small>
        </article>
        <article className="metric-card">
          <span>Prescriptions</span>
          <strong>{prescriptions.length}</strong>
          <small>Medication records available</small>
        </article>
      </div>

      <ErrorMessage message={error} />
      {loading ? <LoadingSpinner label="Loading care timeline" /> : null}

      {!loading ? (
        <div className="two-panel-layout timeline-layout">
          <div className="panel">
            <div className="panel-header">
              <h2>Timeline</h2>
              <Link to="/patient/appointments">Appointments</Link>
            </div>

            {timelineItems.length === 0 ? (
              <div className="empty-inline">No care activity found yet.</div>
            ) : (
              <div className="timeline-list">
                {timelineItems.map((item) => (
                  <article key={item.id} className="timeline-item">
                    <div className="timeline-date">
                      <strong>{formatTimelineDate(item.dateTime)}</strong>
                      <span>{formatTimelineTime(item.dateTime)}</span>
                    </div>
                    <div className="timeline-body">
                      <div className="timeline-header">
                        <span className="soft-badge">{item.label}</span>
                        {item.status ? <StatusBadge status={item.status} /> : null}
                      </div>
                      <h3>{item.title}</h3>
                      <p>{item.subtitle}</p>
                      <p>{item.description}</p>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>

          <div className="panel">
            <div className="panel-header">
              <h2>Medication Snapshot</h2>
              <Link to="/patient/prescriptions">View all</Link>
            </div>

            {prescriptions.length === 0 ? (
              <div className="empty-inline">No prescriptions found.</div>
            ) : (
              <div className="list-stack">
                {prescriptions.slice(0, 6).map((prescription) => (
                  <article key={prescription.id} className="compact-clinical-card">
                    <h3>{prescription.medicationName}</h3>
                    <p>
                      {prescription.dosage} - {prescription.frequency}
                    </p>
                    <p>{prescription.duration}</p>
                  </article>
                ))}
              </div>
            )}
          </div>
        </div>
      ) : null}
    </section>
  );
}
