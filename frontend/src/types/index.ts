export type Role = "PATIENT" | "DOCTOR" | "ADMIN";

export type Gender = "MALE" | "FEMALE" | "OTHER";

export type AppointmentStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";

export type AuditAction =
  | "USER_LOGIN_SUCCESS"
  | "USER_LOGIN_FAILED"
  | "PATIENT_VIEWED"
  | "PATIENT_UPDATED"
  | "DOCTOR_CREATED"
  | "APPOINTMENT_CREATED"
  | "APPOINTMENT_VIEWED"
  | "APPOINTMENT_CANCELLED"
  | "ENCOUNTER_CREATED"
  | "ENCOUNTER_VIEWED"
  | "DIAGNOSIS_CREATED"
  | "PRESCRIPTION_CREATED"
  | "PRESCRIPTION_VIEWED"
  | "SYMPTOM_PATTERN_PREDICTED";

export type SymptomAgeGroup = "CHILD" | "ADULT" | "OLDER_ADULT";

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
  profileId: number | null;
  email: string;
  role: Role;
}

export interface AuthState extends AuthResponse {
  isAuthenticated: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: Gender;
  phone: string;
  address: string;
  emergencyContact: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface Patient {
  id: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  gender: Gender;
  phone: string;
  address: string;
  emergencyContact: string;
}

export interface PatientUpdateRequest {
  firstName: string;
  lastName: string;
  phone: string;
  address: string;
  emergencyContact: string;
}

export interface Doctor {
  id: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  specialization: string;
  licenseNumber: string;
  active: boolean;
}

export interface CreateDoctorRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  specialization: string;
  licenseNumber: string;
}

export interface AppointmentRequest {
  doctorId: number;
  appointmentDateTime: string;
  reason: string;
}

export interface Appointment {
  id: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  doctorSpecialization: string;
  appointmentDateTime: string;
  status: AppointmentStatus;
  reason: string;
  createdAt: string;
}

export interface AppointmentSlot {
  appointmentDateTime: string;
  displayTime: string;
}

export interface EncounterRequest {
  appointmentId: number;
  chiefComplaint: string;
  notes: string;
}

export interface Encounter {
  id: number;
  appointmentId: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  visitDate: string;
  chiefComplaint: string;
  notes: string | null;
}

export interface DiagnosisRequest {
  diagnosisCode: string;
  diagnosisName: string;
  description: string;
}

export interface Diagnosis {
  id: number;
  encounterId: number;
  diagnosisCode: string;
  diagnosisName: string;
  description: string | null;
}

export interface PrescriptionRequest {
  medicationName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions: string;
}

export interface Prescription {
  id: number;
  encounterId: number;
  medicationName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions: string | null;
}

export interface SymptomPredictionRequest {
  fever: number;
  cough: number;
  soreThroat: number;
  runnyNose: number;
  sneezing: number;
  headache: number;
  fatigue: number;
  nausea: number;
  vomiting: number;
  abdominalPain: number;
  diarrhea: number;
  chestDiscomfort: number;
  shortnessOfBreath: number;
  bodyAche: number;
  jointPain: number;
  dizziness: number;
  lightSensitivity: number;
  symptomDurationDays: number;
  ageGroup: SymptomAgeGroup | string;
  encounterId?: number;
}

export interface PatternConfidence {
  pattern: string;
  confidence: number;
}

export interface SymptomCauseConfidence {
  cause: string;
  confidence: number;
}

export interface SymptomFactor {
  field: string;
  label: string;
  value: string;
  contribution: number;
}

export interface SymptomSafetyFlag {
  code: string;
  severity: string;
  message: string;
}

export interface SymptomPossibleCause {
  title: string;
  reason: string;
}

export interface SymptomLikelyCause {
  code: string;
  title: string;
  confidence: number;
  confidenceLevel: "LOW" | "MEDIUM" | "HIGH" | string;
  evidence: string[];
  uncertaintyNotes: string[];
  nextSteps: string[];
}

export interface SymptomPrediction {
  predictedPattern: string;
  confidence: number;
  alternatives: PatternConfidence[];
  likelyCause: SymptomLikelyCause;
  causeAlternatives: SymptomCauseConfidence[];
  modelVersion: string;
  confidenceLevel: "LOW" | "MEDIUM" | "HIGH";
  contributingFactors: SymptomFactor[];
  safetyFlags: SymptomSafetyFlag[];
  possibleCauses: SymptomPossibleCause[];
  suggestedDoctorQuestions: string[];
  reviewPriority: string;
  reviewMessage: string;
  disclaimer: string;
  encounterId: number | null;
}

export interface SymptomPredictionHealth {
  status: "UP" | "DOWN";
  message: string;
}

export interface SymptomModelInfo {
  modelName: string;
  modelType: string;
  modelVersion: string;
  serverMode: string;
  featureCount: number;
  patternCount: number;
  supportedPatterns: string[];
  trainingData: string;
  explanationMethod: string;
  disclaimer: string;
}

export interface AuditLog {
  id: number;
  userId: number;
  userEmail: string;
  action: AuditAction;
  entityType: string;
  entityId: number;
  timestamp: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string>;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
