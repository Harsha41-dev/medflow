import { type CSSProperties, type FormEvent, useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { symptomPredictionApi } from "../../api/symptomPredictionApi";
import { ErrorMessage } from "../../components/ErrorMessage";
import type { PatternConfidence, SymptomModelInfo, SymptomPrediction, SymptomPredictionRequest } from "../../types";
import { getApiErrorMessage } from "../../utils/apiError";

type SymptomKey =
  | "fever"
  | "cough"
  | "soreThroat"
  | "runnyNose"
  | "sneezing"
  | "headache"
  | "fatigue"
  | "nausea"
  | "vomiting"
  | "abdominalPain"
  | "diarrhea"
  | "chestDiscomfort"
  | "shortnessOfBreath"
  | "bodyAche"
  | "jointPain"
  | "dizziness"
  | "lightSensitivity";

type HealthStatus = "CHECKING" | "UP" | "DOWN";

interface SymptomPreset {
  label: string;
  description: string;
  values: Partial<SymptomPredictionRequest>;
}

const symptoms: Array<{ key: SymptomKey; label: string; group: string }> = [
  { key: "fever", label: "Fever", group: "General" },
  { key: "cough", label: "Cough", group: "Respiratory" },
  { key: "soreThroat", label: "Sore throat", group: "Respiratory" },
  { key: "runnyNose", label: "Runny nose", group: "Respiratory" },
  { key: "sneezing", label: "Sneezing", group: "Respiratory" },
  { key: "headache", label: "Headache", group: "Neurologic" },
  { key: "fatigue", label: "Fatigue", group: "General" },
  { key: "nausea", label: "Nausea", group: "Digestive" },
  { key: "vomiting", label: "Vomiting", group: "Digestive" },
  { key: "abdominalPain", label: "Abdominal pain", group: "Digestive" },
  { key: "diarrhea", label: "Diarrhea", group: "Digestive" },
  { key: "chestDiscomfort", label: "Chest discomfort", group: "Safety" },
  { key: "shortnessOfBreath", label: "Shortness of breath", group: "Safety" },
  { key: "bodyAche", label: "Body ache", group: "General" },
  { key: "jointPain", label: "Joint pain", group: "General" },
  { key: "dizziness", label: "Dizziness", group: "Neurologic" },
  { key: "lightSensitivity", label: "Light sensitivity", group: "Neurologic" }
];

const emptyForm: SymptomPredictionRequest = {
  fever: 0,
  cough: 0,
  soreThroat: 0,
  runnyNose: 0,
  sneezing: 0,
  headache: 0,
  fatigue: 0,
  nausea: 0,
  vomiting: 0,
  abdominalPain: 0,
  diarrhea: 0,
  chestDiscomfort: 0,
  shortnessOfBreath: 0,
  bodyAche: 0,
  jointPain: 0,
  dizziness: 0,
  lightSensitivity: 0,
  symptomDurationDays: 3,
  ageGroup: "ADULT"
};

const presets: SymptomPreset[] = [
  {
    label: "Respiratory",
    description: "Cough, throat symptoms, and breathing discomfort",
    values: {
      fever: 1,
      cough: 1,
      soreThroat: 1,
      fatigue: 1,
      chestDiscomfort: 1,
      shortnessOfBreath: 1,
      symptomDurationDays: 4,
      ageGroup: "ADULT"
    }
  },
  {
    label: "Allergy-like",
    description: "Sneezing, runny nose, and mild cough",
    values: {
      cough: 1,
      runnyNose: 1,
      sneezing: 1,
      fatigue: 1,
      symptomDurationDays: 2,
      ageGroup: "ADULT"
    }
  },
  {
    label: "Gastrointestinal",
    description: "Nausea, vomiting, abdominal pain, and diarrhea",
    values: {
      fatigue: 1,
      nausea: 1,
      vomiting: 1,
      abdominalPain: 1,
      diarrhea: 1,
      symptomDurationDays: 2,
      ageGroup: "ADULT"
    }
  },
  {
    label: "Migraine-like",
    description: "Headache with light sensitivity and dizziness",
    values: {
      headache: 1,
      nausea: 1,
      dizziness: 1,
      lightSensitivity: 1,
      symptomDurationDays: 1,
      ageGroup: "ADULT"
    }
  }
];

function formatConfidence(confidence: number): string {
  return Math.round(confidence * 100) + "%";
}

function confidenceWidth(confidence: number): string {
  const boundedConfidence = Math.max(0, Math.min(1, confidence));
  return Math.round(boundedConfidence * 100) + "%";
}

function predictionRows(prediction: SymptomPrediction): PatternConfidence[] {
  return [
    {
      pattern: prediction.predictedPattern,
      confidence: prediction.confidence
    },
    ...prediction.alternatives
  ];
}

function factorContribution(value: number): string {
  const sign = value > 0 ? "+" : "";
  return sign + value.toFixed(3);
}

function factorWidth(value: number): string {
  return Math.min(100, Math.max(18, Math.round(Math.abs(value) * 100))) + "%";
}

function formatPatternName(value: string): string {
  return value
    .replace(/_PATTERN$/i, "")
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}

function formatCauseCode(value: string): string {
  return value
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}

function formatReviewPriority(value: string): string {
  if (!value) {
    return "Clinical review";
  }

  return value
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}

function reviewPriorityClass(value: string): string {
  const priority = value.toLowerCase();

  if (priority.includes("priority")) {
    return "priority-high";
  }

  if (priority.includes("information")) {
    return "priority-medium";
  }

  return "priority-routine";
}

function selectedSymptomLabels(form: SymptomPredictionRequest): string[] {
  return symptoms.filter((symptom) => form[symptom.key] === 1).map((symptom) => symptom.label);
}

function ageGroupLabel(value: string): string {
  if (value === "OLDER_ADULT") {
    return "Older adult";
  }

  return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}

function scoreRingStyle(confidence: number): CSSProperties {
  return {
    background: `conic-gradient(var(--primary) ${confidenceWidth(confidence)}, #dbe7ef 0)`
  };
}

export function SymptomPatternClassifierPage() {
  const [searchParams] = useSearchParams();
  const [form, setForm] = useState<SymptomPredictionRequest>(emptyForm);
  const [prediction, setPrediction] = useState<SymptomPrediction | null>(null);
  const [resultInput, setResultInput] = useState<SymptomPredictionRequest | null>(null);
  const [modelInfo, setModelInfo] = useState<SymptomModelInfo | null>(null);
  const [healthStatus, setHealthStatus] = useState<HealthStatus>("CHECKING");
  const [healthMessage, setHealthMessage] = useState("Checking ML service");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const activeSymptoms = selectedSymptomLabels(form);
  const resultSymptoms = resultInput ? selectedSymptomLabels(resultInput) : activeSymptoms;

  useEffect(() => {
    const encounterId = Number(searchParams.get("encounterId"));
    if (Number.isFinite(encounterId) && encounterId > 0) {
      setForm((current) => Object.assign({}, current, { encounterId }));
    }
  }, [searchParams]);

  useEffect(() => {
    void loadMlStatus();
  }, []);

  async function loadMlStatus() {
    try {
      setHealthStatus("CHECKING");
      setHealthMessage("Checking ML service");
      const [health, info] = await Promise.all([
        symptomPredictionApi.getHealth(),
        symptomPredictionApi.getModelInfo()
      ]);
      setHealthStatus(health.status === "UP" ? "UP" : "DOWN");
      setHealthMessage(health.message);
      setModelInfo(info);
    } catch (apiError) {
      setHealthStatus("DOWN");
      setHealthMessage(getApiErrorMessage(apiError));
      setModelInfo(null);
    }
  }

  function toggleSymptom(key: SymptomKey) {
    setForm((current) => Object.assign({}, current, { [key]: current[key] === 1 ? 0 : 1 }));
  }

  function updateDuration(value: string) {
    const parsedValue = Number(value);
    setForm((current) => Object.assign({}, current, { symptomDurationDays: Number.isFinite(parsedValue) ? parsedValue : 0 }));
  }

  function applyPreset(preset: SymptomPreset) {
    const nextForm = Object.assign({}, emptyForm, preset.values);
    if (form.encounterId) {
      nextForm.encounterId = form.encounterId;
    }
    setForm(nextForm);
    setError("");
    setPrediction(null);
    setResultInput(null);
  }

  function clearSymptoms() {
    const nextForm = Object.assign({}, emptyForm);
    if (form.encounterId) {
      nextForm.encounterId = form.encounterId;
    }
    setForm(nextForm);
    setError("");
    setPrediction(null);
    setResultInput(null);
  }

  async function submitPrediction(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setPrediction(null);

    if (form.symptomDurationDays < 0) {
      setError("Symptom duration must be 0 or greater.");
      return;
    }

    try {
      setSubmitting(true);
      const submittedForm = Object.assign({}, form);
      const result = await symptomPredictionApi.predict(submittedForm);
      setResultInput(submittedForm);
      setPrediction(result);
    } catch (apiError) {
      setResultInput(null);
      setError(getApiErrorMessage(apiError));
      await loadMlStatus();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="content-stack ai-page-shell">
      <div className="ai-workspace-hero">
        <div className="ai-hero-copy">
          <p className="eyebrow">Doctor AI workspace</p>
          <h1>Symptom Pattern Classifier</h1>
          <p>
            A doctor-only decision support screen that turns symptom signals into a readable pattern brief with causes to consider,
            red-flag checks, and model explainability.
          </p>
          <div className="ai-hero-badges" aria-label="Symptom classifier highlights">
            <span>Pre-trained model server</span>
            <span>Doctor-only access</span>
            <span>Audit logged</span>
          </div>
        </div>

        <aside className={"ai-status-panel service-" + healthStatus.toLowerCase()}>
          <div className="ai-status-row">
            <span className="service-dot" aria-hidden="true" />
            <div>
              <strong>ML service</strong>
              <span>{healthMessage}</span>
            </div>
          </div>
          <div className="ai-model-mini-grid">
            <div>
              <span>Model</span>
              <strong>{modelInfo ? modelInfo.modelVersion : "Checking"}</strong>
            </div>
            <div>
              <span>Features</span>
              <strong>{modelInfo ? modelInfo.featureCount : "--"}</strong>
            </div>
            <div>
              <span>Patterns</span>
              <strong>{modelInfo ? modelInfo.patternCount : "--"}</strong>
            </div>
          </div>
          <button type="button" className="button button-secondary button-compact" onClick={loadMlStatus}>
            Refresh service
          </button>
        </aside>
      </div>

      <div className="ai-disclaimer-band">
        <strong>Clinical safety note</strong>
        <span>
          Educational prediction only. This is not a diagnosis, prescription, or treatment recommendation. Final decisions stay with
          the clinician.
        </span>
      </div>

      <ErrorMessage message={error} />

      <div className="ai-workspace-grid">
        <form className="panel ai-input-panel" onSubmit={submitPrediction}>
          <div className="ai-input-header">
            <div>
              <p className="eyebrow">Case input</p>
              <h2>Symptom signals</h2>
            </div>
            <button type="button" className="button button-secondary button-compact" onClick={clearSymptoms}>
              Clear
            </button>
          </div>

          {form.encounterId ? (
            <div className="linked-context">
              Linked encounter ID: <strong>{form.encounterId}</strong>
            </div>
          ) : null}

          <div className="case-strip">
            <div className="case-strip-item">
              <span>Selected</span>
              <strong>{activeSymptoms.length}</strong>
            </div>
            <div className="case-strip-item">
              <span>Duration</span>
              <strong>{form.symptomDurationDays} days</strong>
            </div>
            <div className="case-strip-item">
              <span>Age group</span>
              <strong>{ageGroupLabel(String(form.ageGroup))}</strong>
            </div>
          </div>

          <div className="preset-grid ai-preset-grid full-width">
            {presets.map((preset) => (
              <button key={preset.label} type="button" className="preset-button ai-preset-button" onClick={() => applyPreset(preset)}>
                <strong>{preset.label}</strong>
                <span>{preset.description}</span>
              </button>
            ))}
          </div>

          <div className="selected-symptom-strip">
            <span>Active symptoms</span>
            {activeSymptoms.length > 0 ? (
              <div className="selected-chip-list">
                {activeSymptoms.map((symptom) => (
                  <strong key={symptom} className="selected-chip">
                    {symptom}
                  </strong>
                ))}
              </div>
            ) : (
              <strong className="empty-chip-state">None selected</strong>
            )}
          </div>

          <div className="symptom-grid ai-symptom-grid full-width">
            {symptoms.map((symptom) => (
              <label key={symptom.key} className={form[symptom.key] === 1 ? "symptom-toggle active" : "symptom-toggle"}>
                <input type="checkbox" checked={form[symptom.key] === 1} onChange={() => toggleSymptom(symptom.key)} />
                <span>{symptom.label}</span>
                <small>{symptom.group}</small>
              </label>
            ))}
          </div>

          <div className="form-grid two-columns full-width">
            <label className="field">
              <span>Symptom duration days</span>
              <input
                type="number"
                min="0"
                value={form.symptomDurationDays}
                onChange={(event) => updateDuration(event.target.value)}
                required
              />
            </label>

            <label className="field">
              <span>Age group</span>
              <select value={form.ageGroup} onChange={(event) => setForm((current) => Object.assign({}, current, { ageGroup: event.target.value }))}>
                <option value="CHILD">Child</option>
                <option value="ADULT">Adult</option>
                <option value="OLDER_ADULT">Older adult</option>
              </select>
            </label>
          </div>

          <div className="ai-form-actions">
            <button type="submit" className="button button-primary" disabled={submitting || healthStatus === "DOWN"}>
              {submitting ? "Building clinical brief" : "Generate AI pattern brief"}
            </button>
          </div>
        </form>

        <aside className="ai-side-rail">
          <div className="side-rail-card">
            <p className="eyebrow">Model behavior</p>
            <h2>{modelInfo ? modelInfo.modelType : "Classifier service"}</h2>
            <p>{modelInfo ? modelInfo.explanationMethod : "The backend checks service health before prediction."}</p>
          </div>

          <div className="side-rail-card">
            <p className="eyebrow">Output includes</p>
            <div className="side-step">
              <strong>Likely cause</strong>
              <span>Trained cause ranking with confidence, evidence, and uncertainty notes.</span>
            </div>
            <div className="side-step">
              <strong>Safety flags</strong>
              <span>Red-flag style warnings for higher-risk symptom combinations.</span>
            </div>
            <div className="side-step">
              <strong>Why this result</strong>
              <span>Top contributing inputs returned from the model service.</span>
            </div>
          </div>
        </aside>
      </div>

      {prediction ? (
        <div className="panel ai-result-panel">
          <div className="ai-result-hero">
            <div className="ai-result-copy">
              <p className="eyebrow">AI likely-cause brief</p>
              <h2>{prediction.likelyCause.title}</h2>
              <p>{prediction.likelyCause.evidence.join(", ")}</p>
              <div className="ai-result-meta">
                <span className={"priority-badge " + reviewPriorityClass(prediction.reviewPriority)}>
                  {formatReviewPriority(prediction.reviewPriority)}
                </span>
                <span>{prediction.likelyCause.confidenceLevel} cause confidence</span>
                <span>{formatPatternName(prediction.predictedPattern)} pattern</span>
                <span>{prediction.modelVersion}</span>
              </div>
            </div>

            <div className="ai-score-ring" style={scoreRingStyle(prediction.likelyCause.confidence)} aria-label={"Cause confidence " + formatConfidence(prediction.likelyCause.confidence)}>
              <div className="ai-score-inner">
                <strong>{formatConfidence(prediction.likelyCause.confidence)}</strong>
                <span>Cause confidence</span>
              </div>
            </div>
          </div>

          <div className="ai-result-layout">
            <section className="clinical-brief">
              <div className="section-title-row">
                <div>
                  <p className="eyebrow">Clinical snapshot</p>
                  <h3>Case summary</h3>
                </div>
              </div>

              <div className="brief-grid">
                <div className="brief-card">
                  <span>Duration</span>
                  <strong>{resultInput?.symptomDurationDays ?? form.symptomDurationDays} days</strong>
                </div>
                <div className="brief-card">
                  <span>Age group</span>
                  <strong>{ageGroupLabel(String(resultInput?.ageGroup ?? form.ageGroup))}</strong>
                </div>
                <div className="brief-card">
                  <span>Symptoms</span>
                  <strong>{resultSymptoms.length}</strong>
                </div>
                <div className="brief-card">
                  <span>Pattern confidence</span>
                  <strong>{formatConfidence(prediction.confidence)}</strong>
                </div>
              </div>

              <div className="symptom-summary-card">
                <span>Symptoms submitted</span>
                {resultSymptoms.length > 0 ? (
                  <div className="selected-chip-list">
                    {resultSymptoms.map((symptom) => (
                      <strong key={symptom} className="selected-chip">
                        {symptom}
                      </strong>
                    ))}
                  </div>
                ) : (
                  <strong className="empty-chip-state">No symptoms were selected.</strong>
                )}
              </div>

              <div className="likely-cause-card">
                <div className="likely-cause-header">
                  <div>
                    <p className="eyebrow">Most likely cause to review</p>
                    <h3>{prediction.likelyCause.title}</h3>
                  </div>
                  <span className="cause-code">{formatCauseCode(prediction.likelyCause.code)}</span>
                </div>

                {prediction.likelyCause.evidence.length > 0 ? (
                  <div className="evidence-list">
                    <span>Evidence used</span>
                    {prediction.likelyCause.evidence.map((item) => (
                      <strong key={item}>{item}</strong>
                    ))}
                  </div>
                ) : null}

                {prediction.likelyCause.nextSteps.length > 0 ? (
                  <div className="next-step-list">
                    <span>Doctor next steps</span>
                    {prediction.likelyCause.nextSteps.map((step) => (
                      <p key={step}>{step}</p>
                    ))}
                  </div>
                ) : null}

                {prediction.likelyCause.uncertaintyNotes.length > 0 ? (
                  <div className="uncertainty-list">
                    <span>Uncertainty</span>
                    {prediction.likelyCause.uncertaintyNotes.map((note) => (
                      <p key={note}>{note}</p>
                    ))}
                  </div>
                ) : null}
              </div>

              {prediction.safetyFlags.length > 0 ? (
                <div className="safety-spotlight">
                  <div>
                    <p className="eyebrow">Safety review</p>
                    <h3>{prediction.safetyFlags.length} flag detected</h3>
                  </div>
                  <div className="safety-list">
                    {prediction.safetyFlags.map((flag) => (
                      <div key={flag.code} className="safety-item">
                        <strong>{flag.severity}</strong>
                        <span>{flag.message}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}

              {prediction.possibleCauses.length > 0 ? (
                <div className="cause-section">
                  <div className="section-title-row">
                    <div>
                      <p className="eyebrow">Differential thinking</p>
                      <h3>Possible causes to consider</h3>
                    </div>
                  </div>

                  <div className="insight-grid">
                    {prediction.possibleCauses.map((cause, index) => (
                      <article key={cause.title} className="cause-card">
                        <span className="cause-index">{String(index + 1).padStart(2, "0")}</span>
                        <div>
                          <strong>{cause.title}</strong>
                          <p>{cause.reason}</p>
                        </div>
                      </article>
                    ))}
                  </div>
                </div>
              ) : null}
            </section>

            <aside className="clinical-brief">
              {prediction.suggestedDoctorQuestions.length > 0 ? (
                <div className="question-list question-card">
                  <div>
                    <p className="eyebrow">Consultation prompts</p>
                    <h3>Suggested follow-up questions</h3>
                  </div>
                  {prediction.suggestedDoctorQuestions.map((question, index) => (
                    <div key={question} className="question-item">
                      <span className="question-number">{index + 1}</span>
                      <strong>{question}</strong>
                    </div>
                  ))}
                </div>
              ) : null}

              {prediction.causeAlternatives.length > 0 ? (
                <div className="confidence-section">
                  <div>
                    <p className="eyebrow">Cause ranking</p>
                    <h3>Alternative signals</h3>
                  </div>
                  <div className="confidence-bars">
                    {prediction.causeAlternatives.map((row) => (
                      <div key={row.cause} className="confidence-bar-item">
                        <div className="confidence-labels">
                          <span>{formatCauseCode(row.cause)}</span>
                          <strong>{formatConfidence(row.confidence)}</strong>
                        </div>
                        <div className="confidence-bar-track" aria-hidden="true">
                          <span className="confidence-bar-fill confidence-bar-cause" style={{ width: confidenceWidth(row.confidence) }} />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}

              <div className="confidence-section">
                <div>
                  <p className="eyebrow">Pattern confidence</p>
                  <h3>Top matches</h3>
                </div>
                <div className="confidence-bars">
                  {predictionRows(prediction).map((row) => (
                    <div key={row.pattern} className="confidence-bar-item">
                      <div className="confidence-labels">
                        <span>{formatPatternName(row.pattern)}</span>
                        <strong>{formatConfidence(row.confidence)}</strong>
                      </div>
                      <div className="confidence-bar-track" aria-hidden="true">
                        <span className="confidence-bar-fill" style={{ width: confidenceWidth(row.confidence) }} />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {prediction.contributingFactors.length > 0 ? (
                <div className="factor-list">
                  <div>
                    <p className="eyebrow">Explainability</p>
                    <h3>Top model factors</h3>
                  </div>
                  <div className="factor-grid">
                    {prediction.contributingFactors.map((factor) => (
                      <div key={factor.field + factor.value} className="factor-card">
                        <div className="factor-card-header">
                          <div>
                            <strong>{factor.label}</strong>
                            <span>{factor.value}</span>
                          </div>
                          <code>{factorContribution(factor.contribution)}</code>
                        </div>
                        <div className="factor-bar-track" aria-hidden="true">
                          <span className="factor-bar-fill" style={{ width: factorWidth(factor.contribution) }} />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}
            </aside>
          </div>

          <p className="muted-text">{prediction.disclaimer}</p>
        </div>
      ) : null}
    </section>
  );
}
