import { Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { HomeRedirect } from "./routes/HomeRedirect";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import { AdminAuditLogsPage } from "./pages/admin/AdminAuditLogsPage";
import { AdminDashboardPage } from "./pages/admin/AdminDashboardPage";
import { AdminDoctorsPage } from "./pages/admin/AdminDoctorsPage";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { UnauthorizedPage } from "./pages/auth/UnauthorizedPage";
import { DoctorAppointmentsPage } from "./pages/doctor/DoctorAppointmentsPage";
import { DoctorDashboardPage } from "./pages/doctor/DoctorDashboardPage";
import { DoctorEncounterPage } from "./pages/doctor/DoctorEncounterPage";
import { DoctorPatientRecordPage } from "./pages/doctor/DoctorPatientRecordPage";
import { DoctorPatientsPage } from "./pages/doctor/DoctorPatientsPage";
import { SymptomPatternClassifierPage } from "./pages/doctor/SymptomPatternClassifierPage";
import { BookAppointmentPage } from "./pages/patient/BookAppointmentPage";
import { PatientAppointmentsPage } from "./pages/patient/PatientAppointmentsPage";
import { PatientCareTimelinePage } from "./pages/patient/PatientCareTimelinePage";
import { PatientDashboardPage } from "./pages/patient/PatientDashboardPage";
import { PatientDoctorsPage } from "./pages/patient/PatientDoctorsPage";
import { PatientMedicalHistoryPage } from "./pages/patient/PatientMedicalHistoryPage";
import { PatientPrescriptionsPage } from "./pages/patient/PatientPrescriptionsPage";
import { PatientProfilePage } from "./pages/patient/PatientProfilePage";

function NotFoundPage() {
  return (
    <section className="empty-state">
      <h1>Page not found</h1>
      <p>The page you requested does not exist in MedFlow.</p>
    </section>
  );
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        <Route element={<ProtectedRoute roles={["PATIENT"]} />}>
          <Route path="/patient/dashboard" element={<PatientDashboardPage />} />
          <Route path="/patient/profile" element={<PatientProfilePage />} />
          <Route path="/patient/doctors" element={<PatientDoctorsPage />} />
          <Route path="/patient/appointments" element={<PatientAppointmentsPage />} />
          <Route path="/patient/book-appointment" element={<BookAppointmentPage />} />
          <Route path="/patient/timeline" element={<PatientCareTimelinePage />} />
          <Route path="/patient/medical-history" element={<PatientMedicalHistoryPage />} />
          <Route path="/patient/prescriptions" element={<PatientPrescriptionsPage />} />
        </Route>

        <Route element={<ProtectedRoute roles={["DOCTOR"]} />}>
          <Route path="/doctor/dashboard" element={<DoctorDashboardPage />} />
          <Route path="/doctor/appointments" element={<DoctorAppointmentsPage />} />
          <Route path="/doctor/patients" element={<DoctorPatientsPage />} />
          <Route path="/doctor/patients/:patientId" element={<DoctorPatientRecordPage />} />
          <Route path="/doctor/encounters/:encounterId" element={<DoctorEncounterPage />} />
          <Route path="/doctor/symptom-classifier" element={<SymptomPatternClassifierPage />} />
        </Route>

        <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
          <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
          <Route path="/admin/doctors" element={<AdminDoctorsPage />} />
          <Route path="/admin/audit-logs" element={<AdminAuditLogsPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
