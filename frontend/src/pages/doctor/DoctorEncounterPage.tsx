import { type FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { clinicalApi } from "../../api/clinicalApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import { LoadingSpinner } from "../../components/LoadingSpinner";
import type { Diagnosis, DiagnosisRequest, Encounter, Prescription, PrescriptionRequest } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";
import { formatDateTime } from "../../utils/dateTime";

const emptyDiagnosisForm: DiagnosisRequest = {
  diagnosisCode: "",
  diagnosisName: "",
  description: ""
};

const emptyPrescriptionForm: PrescriptionRequest = {
  medicationName: "",
  dosage: "",
  frequency: "",
  duration: "",
  instructions: ""
};

export function DoctorEncounterPage() {
  const { encounterId } = useParams();
  const numericEncounterId = Number(encounterId);
  const [encounter, setEncounter] = useState<Encounter | null>(null);
  const [diagnosisForm, setDiagnosisForm] = useState<DiagnosisRequest>(emptyDiagnosisForm);
  const [prescriptionForm, setPrescriptionForm] = useState<PrescriptionRequest>(emptyPrescriptionForm);
  const [createdDiagnoses, setCreatedDiagnoses] = useState<Diagnosis[]>([]);
  const [createdPrescriptions, setCreatedPrescriptions] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingDiagnosis, setSavingDiagnosis] = useState(false);
  const [savingPrescription, setSavingPrescription] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadEncounter() {
      if (!Number.isFinite(numericEncounterId)) {
        setError("Encounter ID is invalid.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const data = await clinicalApi.getEncounter(numericEncounterId);
        setEncounter(data);
      } catch (apiError) {
        setError(getApiErrorMessage(apiError));
      } finally {
        setLoading(false);
      }
    }

    loadEncounter();
  }, [numericEncounterId]);

  function updateDiagnosisField(field: keyof DiagnosisRequest, value: string) {
    setDiagnosisForm((current) => Object.assign({}, current, { [field]: value }));
  }

  function updatePrescriptionField(field: keyof PrescriptionRequest, value: string) {
    setPrescriptionForm((current) => Object.assign({}, current, { [field]: value }));
  }

  async function submitDiagnosis(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (!diagnosisForm.diagnosisCode.trim() || !diagnosisForm.diagnosisName.trim()) {
      setError("Diagnosis code and name are required.");
      return;
    }

    try {
      setSavingDiagnosis(true);
      const diagnosis = await clinicalApi.addDiagnosis(numericEncounterId, {
        diagnosisCode: diagnosisForm.diagnosisCode.trim(),
        diagnosisName: diagnosisForm.diagnosisName.trim(),
        description: diagnosisForm.description.trim()
      });
      setCreatedDiagnoses((current) => current.concat(diagnosis));
      setDiagnosisForm(emptyDiagnosisForm);
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setSavingDiagnosis(false);
    }
  }

  async function submitPrescription(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (
      !prescriptionForm.medicationName.trim() ||
      !prescriptionForm.dosage.trim() ||
      !prescriptionForm.frequency.trim() ||
      !prescriptionForm.duration.trim()
    ) {
      setError("Medication, dosage, frequency, and duration are required.");
      return;
    }

    try {
      setSavingPrescription(true);
      const prescription = await clinicalApi.addPrescription(numericEncounterId, {
        medicationName: prescriptionForm.medicationName.trim(),
        dosage: prescriptionForm.dosage.trim(),
        frequency: prescriptionForm.frequency.trim(),
        duration: prescriptionForm.duration.trim(),
        instructions: prescriptionForm.instructions.trim()
      });
      setCreatedPrescriptions((current) => current.concat(prescription));
      setPrescriptionForm(emptyPrescriptionForm);
    } catch (apiError) {
      setError(getApiErrorMessage(apiError));
    } finally {
      setSavingPrescription(false);
    }
  }

  if (loading) {
    return <LoadingSpinner label="Loading encounter" />;
  }

  return (
    <section className="content-stack">
      <div className="section-heading">
        <p className="eyebrow">Encounter</p>
        <h1>Encounter Details</h1>
        <p>Add diagnosis and prescription records for the selected encounter.</p>
      </div>

      <ErrorMessage message={error} />

      {encounter ? (
        <>
          <div className="panel">
            <h2>{encounter.chiefComplaint}</h2>
            <dl className="detail-list">
              <div>
                <dt>Patient</dt>
                <dd>{encounter.patientName}</dd>
              </div>
              <div>
                <dt>Doctor</dt>
                <dd>{encounter.doctorName}</dd>
              </div>
              <div>
                <dt>Visit date</dt>
                <dd>{formatDateTime(encounter.visitDate)}</dd>
              </div>
              <div>
                <dt>Notes</dt>
                <dd>{encounter.notes || "Not provided"}</dd>
              </div>
            </dl>
            <div className="button-row encounter-tools">
              <Link className="button button-secondary" to={`/doctor/symptom-classifier?encounterId=${encounter.id}`}>
                Open symptom classifier
              </Link>
            </div>
          </div>

          <div className="two-panel-layout">
            <form className="panel form-grid" onSubmit={submitDiagnosis}>
              <h2>Add diagnosis</h2>
              <label className="field">
                <span>Diagnosis code</span>
                <input
                  value={diagnosisForm.diagnosisCode}
                  onChange={(event) => updateDiagnosisField("diagnosisCode", event.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Diagnosis name</span>
                <input
                  value={diagnosisForm.diagnosisName}
                  onChange={(event) => updateDiagnosisField("diagnosisName", event.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Description</span>
                <textarea
                  value={diagnosisForm.description}
                  onChange={(event) => updateDiagnosisField("description", event.target.value)}
                  rows={3}
                />
              </label>
              <button type="submit" className="button button-primary" disabled={savingDiagnosis}>
                {savingDiagnosis ? "Saving diagnosis" : "Add diagnosis"}
              </button>
            </form>

            <form className="panel form-grid" onSubmit={submitPrescription}>
              <h2>Add prescription</h2>
              <label className="field">
                <span>Medication</span>
                <input
                  value={prescriptionForm.medicationName}
                  onChange={(event) => updatePrescriptionField("medicationName", event.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Dosage</span>
                <input value={prescriptionForm.dosage} onChange={(event) => updatePrescriptionField("dosage", event.target.value)} required />
              </label>
              <label className="field">
                <span>Frequency</span>
                <input
                  value={prescriptionForm.frequency}
                  onChange={(event) => updatePrescriptionField("frequency", event.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Duration</span>
                <input
                  value={prescriptionForm.duration}
                  onChange={(event) => updatePrescriptionField("duration", event.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Instructions</span>
                <textarea
                  value={prescriptionForm.instructions}
                  onChange={(event) => updatePrescriptionField("instructions", event.target.value)}
                  rows={3}
                />
              </label>
              <button type="submit" className="button button-primary" disabled={savingPrescription}>
                {savingPrescription ? "Saving prescription" : "Add prescription"}
              </button>
            </form>
          </div>

          <div className="two-panel-layout">
            <div className="panel">
              <h2>Diagnoses added in this session</h2>
              {createdDiagnoses.length === 0 ? <div className="empty-inline">No diagnoses added yet.</div> : null}
              {createdDiagnoses.map((diagnosis) => (
                <article key={diagnosis.id} className="compact-record">
                  <strong>{diagnosis.diagnosisCode}</strong>
                  <span>{diagnosis.diagnosisName}</span>
                </article>
              ))}
            </div>

            <div className="panel">
              <h2>Prescriptions added in this session</h2>
              {createdPrescriptions.length === 0 ? <div className="empty-inline">No prescriptions added yet.</div> : null}
              {createdPrescriptions.map((prescription) => (
                <article key={prescription.id} className="compact-record">
                  <strong>{prescription.medicationName}</strong>
                  <span>{prescription.dosage}</span>
                </article>
              ))}
            </div>
          </div>
        </>
      ) : null}
    </section>
  );
}
