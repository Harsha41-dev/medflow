import { type FormEvent, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { appointmentApi } from "../../api/appointmentApi";
import { doctorApi } from "../../api/doctorApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import type { AppointmentSlot, Doctor } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { isFutureDateTime, isThirtyMinuteSlot, normalizeDateTimeForBackend } from "../../utils/dateTime";

export function BookAppointmentPage() {
  const [searchParams] = useSearchParams();
  const requestedDoctorId = searchParams.get("doctorId") || "";
  const requestedSpecialization = searchParams.get("specialization") || "";
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [doctorId, setDoctorId] = useState(requestedDoctorId);
  const [appointmentDate, setAppointmentDate] = useState(getDefaultAppointmentDate());
  const [appointmentDateTime, setAppointmentDateTime] = useState("");
  const [availableSlots, setAvailableSlots] = useState<AppointmentSlot[]>([]);
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [slotError, setSlotError] = useState("");
  const [success, setSuccess] = useState("");

  const selectedDoctor = useMemo(
    () => doctors.find((doctor) => doctor.id === Number(doctorId)),
    [doctorId, doctors]
  );

  useEffect(() => {
    async function loadDoctors() {
      try {
        setLoading(true);
        const page = await doctorApi.listDoctors({ page: 0, size: 100 });
        setDoctors(page.content);
      } catch (apiError) {
        setError(getApiErrorMessage(apiError));
      } finally {
        setLoading(false);
      }
    }

    loadDoctors();
  }, []);

  useEffect(() => {
    if (requestedDoctorId) {
      setDoctorId(requestedDoctorId);
      return;
    }

    if (!requestedSpecialization || doctors.length === 0) {
      return;
    }

    const matchingDoctor = doctors.find((doctor) => {
      return doctor.specialization.toLowerCase() === requestedSpecialization.toLowerCase();
    });

    if (matchingDoctor) {
      setDoctorId(String(matchingDoctor.id));
    }
  }, [doctors, requestedDoctorId, requestedSpecialization]);

  useEffect(() => {
    async function loadAvailableSlots() {
      if (!doctorId || !appointmentDate) {
        setAvailableSlots([]);
        setAppointmentDateTime("");
        return;
      }

      try {
        setSlotsLoading(true);
        setSlotError("");
        setAppointmentDateTime("");
        const slots = await appointmentApi.listAvailableSlots(Number(doctorId), appointmentDate);
        setAvailableSlots(slots);
      } catch (apiError) {
        setAvailableSlots([]);
        setSlotError(getApiErrorMessage(apiError));
      } finally {
        setSlotsLoading(false);
      }
    }

    loadAvailableSlots();
  }, [doctorId, appointmentDate]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!doctorId || !appointmentDateTime || !reason.trim()) {
      setError("Doctor, appointment slot, and reason are required.");
      return;
    }

    if (!isFutureDateTime(appointmentDateTime)) {
      setError("Appointment time must be in the future.");
      return;
    }

    if (!isThirtyMinuteSlot(appointmentDateTime)) {
      setError("Choose a time ending in :00 or :30.");
      return;
    }

    try {
      setSubmitting(true);
      const normalizedDateTime = normalizeDateTimeForBackend(appointmentDateTime);
      await appointmentApi.bookAppointment({
        doctorId: Number(doctorId),
        appointmentDateTime: normalizedDateTime,
        reason: reason.trim()
      });
      setSuccess("Appointment booked successfully.");
      setAvailableSlots((currentSlots) =>
        currentSlots.filter((slot) => normalizeDateTimeForBackend(slot.appointmentDateTime) !== normalizedDateTime)
      );
      setAppointmentDateTime("");
      setReason("");
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Book appointment</p>
        <h1>Choose a Doctor and Slot</h1>
        <p>Your patient identity is read from your login token.</p>
      </div>

      <ErrorMessage message={error} />
      {success ? <div className="alert alert-success">{success}</div> : null}
      {loading ? <LoadingSpinner label="Loading doctors" /> : null}

      {!loading ? (
        <form className="booking-grid" onSubmit={handleSubmit}>
          <div className="panel form-grid booking-controls">
            <label className="field">
              <span>Doctor</span>
              <select value={doctorId} onChange={(event) => setDoctorId(event.target.value)} required>
                <option value="">Select doctor</option>
                {doctors.map((doctor) => (
                  <option key={doctor.id} value={doctor.id}>
                    Dr. {doctor.firstName} {doctor.lastName} - {doctor.specialization}
                  </option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>Date</span>
              <input
                type="date"
                min={getTodayDate()}
                value={appointmentDate}
                onChange={(event) => setAppointmentDate(event.target.value)}
                required
              />
            </label>

            {selectedDoctor ? (
              <div className="selected-doctor-strip">
                <strong>
                  Dr. {selectedDoctor.firstName} {selectedDoctor.lastName}
                </strong>
                <span>{selectedDoctor.specialization}</span>
              </div>
            ) : null}

            {!selectedDoctor && requestedSpecialization ? (
              <div className="selected-doctor-strip">
                <strong>{requestedSpecialization}</strong>
                <span>No exact doctor match was found. Choose another available doctor.</span>
              </div>
            ) : null}

            <label className="field">
              <span>Reason</span>
              <textarea value={reason} onChange={(event) => setReason(event.target.value)} rows={4} required />
            </label>

            <button type="submit" className="button button-primary" disabled={submitting || !appointmentDateTime}>
              {submitting ? "Booking" : "Book appointment"}
            </button>
          </div>

          <div className="panel slot-picker-panel">
            <div className="panel-header">
              <div>
                <h2>Available slots</h2>
                <p>{appointmentDate ? formatSelectedDate(appointmentDate) : "No date selected"}</p>
              </div>
              <span className="slot-count">{availableSlots.length}</span>
            </div>

            <ErrorMessage message={slotError} />
            {slotsLoading ? <LoadingSpinner label="Loading slots" /> : null}

            {!slotsLoading && !doctorId ? <div className="empty-inline">No doctor selected.</div> : null}

            {!slotsLoading && doctorId && availableSlots.length === 0 && !slotError ? (
              <div className="empty-inline">No open slots for this date.</div>
            ) : null}

            {!slotsLoading && availableSlots.length > 0 ? (
              <div className="slot-grid">
                {availableSlots.map((slot) => (
                  <button
                    type="button"
                    key={slot.appointmentDateTime}
                    className={appointmentDateTime === slot.appointmentDateTime ? "slot-button selected" : "slot-button"}
                    onClick={() => {
                      setAppointmentDateTime(slot.appointmentDateTime);
                      setSuccess("");
                    }}
                  >
                    {slot.displayTime}
                  </button>
                ))}
              </div>
            ) : null}
          </div>
        </form>
      ) : null}
    </section>
  );
}

function getTodayDate(): string {
  return toDateInputValue(new Date());
}

function getDefaultAppointmentDate(): string {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return toDateInputValue(date);
}

function toDateInputValue(date: Date): string {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 10);
}

function formatSelectedDate(value: string): string {
  const date = new Date(value + "T00:00:00");
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
    year: "numeric"
  });
}
