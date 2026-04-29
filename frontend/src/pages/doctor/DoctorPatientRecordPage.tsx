import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { appointmentApi } from "../../api/appointmentApi";
import { clinicalApi } from "../../api/clinicalApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import { StatusBadge } from "../../components/StatusBadge";
import type { Appointment, Encounter, Prescription } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDateTime } from "../../utils/dateTime";

function toTime(value: string): number {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 0 : date.getTime();
}

export function DoctorPatientRecordPage() {
  const { patientId } = useParams();
  const numericPatientId = Number(patientId);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [encounters, setEncounters] = useState<Encounter[]>([]);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadRecord() {
      if (!Number.isInteger(numericPatientId) || numericPatientId <= 0) {
        setError("Patient record was not found.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError("");
        const [appointmentPage, encounterPage, prescriptionPage] = await Promise.all([
          appointmentApi.listAppointments({ status: "", page: 0, size: 100 }),
          clinicalApi.listPatientEncounters(numericPatientId, 0, 20),
          clinicalApi.listPatientPrescriptions(numericPatientId, 0, 20)
        ]);

        setAppointments(appointmentPage.content.filter((appointment) => appointment.patientId === numericPatientId));
        setEncounters(encounterPage.content);
        setPrescriptions(prescriptionPage.content);
      } catch (apiError) {
        setError(getApiErrorMessage(apiError));
      } finally {
        setLoading(false);
      }
    }

    loadRecord();
  }, [numericPatientId]);

  const sortedAppointments = useMemo(() => {
    return appointments.slice().sort((first, second) => toTime(second.appointmentDateTime) - toTime(first.appointmentDateTime));
  }, [appointments]);

  const sortedEncounters = useMemo(() => {
    return encounters.slice().sort((first, second) => toTime(second.visitDate) - toTime(first.visitDate));
  }, [encounters]);

  const patientName = sortedAppointments[0]?.patientName || sortedEncounters[0]?.patientName || "Patient #" + numericPatientId;
  const scheduledCount = appointments.filter((appointment) => appointment.status === "SCHEDULED").length;
  const completedCount = appointments.filter((appointment) => appointment.status === "COMPLETED").length;
  const latestEncounter = sortedEncounters[0];

  const encounterById = useMemo(() => {
    const map = new Map<number, Encounter>();
    encounters.forEach((encounter) => map.set(encounter.id, encounter));
    return map;
  }, [encounters]);

  return (
    <section className="content-stack">
      <div className="hero-panel hero-doctor">
        <div className="section-heading">
          <p className="eyebrow">Patient record</p>
          <h1>{loading ? "Loading patient record" : patientName}</h1>
          <p>Appointments, encounters, and medicines connected to your doctor account.</p>
        </div>

        <div className="hero-summary-card">
          <span className="summary-label">Latest clinical note</span>
          {loading ? (
            <strong>Checking record</strong>
          ) : latestEncounter ? (
            <>
              <strong>{latestEncounter.chiefComplaint}</strong>
              <span>{formatDateTime(latestEncounter.visitDate)}</span>
            </>
          ) : (
            <>
              <strong>No encounter yet</strong>
              <span>Create one from a scheduled appointment.</span>
            </>
          )}
        </div>
      </div>

      <div className="button-row">
        <Link to="/doctor/patients" className="button button-secondary">
          Back to patients
        </Link>
        <Link to="/doctor/appointments" className="button button-secondary">
          Assigned appointments
        </Link>
      </div>

      <div className="insight-grid">
        <article className="metric-card">
          <span>Appointments</span>
          <strong>{appointments.length}</strong>
          <small>Visible through your assigned schedule</small>
        </article>
        <article className="metric-card">
          <span>Scheduled</span>
          <strong>{scheduledCount}</strong>
          <small>Open visits for this patient</small>
        </article>
        <article className="metric-card">
          <span>Completed</span>
          <strong>{completedCount}</strong>
          <small>Visits that have clinical history</small>
        </article>
        <article className="metric-card">
          <span>Prescriptions</span>
          <strong>{prescriptions.length}</strong>
          <small>Medication entries in this record</small>
        </article>
      </div>

      <ErrorMessage message={error} />
      {loading ? <LoadingSpinner label="Loading patient record" /> : null}

      {!loading && !error ? (
        <>
          <div className="two-panel-layout timeline-layout">
            <div className="panel">
              <div className="panel-header">
                <h2>Appointment History</h2>
              </div>

              {sortedAppointments.length === 0 ? (
                <div className="empty-inline">No appointment history found for this patient.</div>
              ) : (
                <div className="list-stack">
                  {sortedAppointments.map((appointment) => (
                    <article key={appointment.id} className="record-card">
                      <div>
                        <h3>{formatDateTime(appointment.appointmentDateTime)}</h3>
                        <p>{appointment.reason}</p>
                        <p>{appointment.doctorSpecialization}</p>
                      </div>
                      <StatusBadge status={appointment.status} />
                    </article>
                  ))}
                </div>
              )}
            </div>

            <div className="panel">
              <div className="panel-header">
                <h2>Encounter Notes</h2>
              </div>

              {sortedEncounters.length === 0 ? (
                <div className="empty-inline">No encounters recorded yet.</div>
              ) : (
                <div className="list-stack">
                  {sortedEncounters.map((encounter) => (
                    <article key={encounter.id} className="record-card clinical-record-card">
                      <div>
                        <h3>{encounter.chiefComplaint}</h3>
                        <p>{formatDateTime(encounter.visitDate)}</p>
                        <p>{encounter.notes || "No additional notes recorded."}</p>
                      </div>
                      <Link className="button button-secondary button-compact" to={"/doctor/encounters/" + encounter.id}>
                        Open
                      </Link>
                    </article>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="panel">
            <div className="panel-header">
              <h2>Prescriptions</h2>
            </div>

            {prescriptions.length === 0 ? (
              <div className="empty-inline">No prescriptions found.</div>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Medication</th>
                      <th>Dosage</th>
                      <th>Frequency</th>
                      <th>Duration</th>
                      <th>Encounter</th>
                      <th>Instructions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {prescriptions.map((prescription) => {
                      const linkedEncounter = encounterById.get(prescription.encounterId);

                      return (
                        <tr key={prescription.id}>
                          <td>{prescription.medicationName}</td>
                          <td>{prescription.dosage}</td>
                          <td>{prescription.frequency}</td>
                          <td>{prescription.duration}</td>
                          <td>{linkedEncounter?.chiefComplaint || "Encounter #" + prescription.encounterId}</td>
                          <td>{prescription.instructions || "Not provided"}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      ) : null}
    </section>
  );
}
