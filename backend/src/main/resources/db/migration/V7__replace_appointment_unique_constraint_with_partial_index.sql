ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS uk_appointments_doctor_time;

CREATE UNIQUE INDEX IF NOT EXISTS ux_appointments_scheduled_doctor_time
    ON appointments (doctor_id, appointment_date_time)
    WHERE status = 'SCHEDULED';
