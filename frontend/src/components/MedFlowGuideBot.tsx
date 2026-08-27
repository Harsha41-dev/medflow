import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import type { AuthState, Role } from "../types";

type MessageOwner = "bot" | "user";

interface GuideAction {
  label: string;
  path: string;
}

interface GuideMessage {
  id: number;
  owner: MessageOwner;
  text: string;
  actions?: GuideAction[];
}

interface SpecialtyMatch {
  specialty: string;
  reason: string;
  keywords: string[];
  path: string;
}

interface GuideIntro {
  title: string;
  body: string;
}

const specialtyMatches: SpecialtyMatch[] = [
  {
    specialty: "Cardiology",
    reason: "Chest discomfort, palpitations, high blood pressure, and breathing symptoms may need a heart-focused review.",
    keywords: ["chest", "heart", "palpitation", "blood pressure", "bp", "breath", "shortness"],
    path: "/patient/book-appointment?specialization=Cardiology"
  },
  {
    specialty: "Dermatology",
    reason: "Skin rashes, acne, itching, and visible skin changes are usually handled by a dermatologist.",
    keywords: ["rash", "skin", "itch", "acne", "allergy", "spots", "eczema"],
    path: "/patient/book-appointment?specialization=Dermatology"
  },
  {
    specialty: "Orthopedics",
    reason: "Joint pain, back pain, sprains, fractures, and movement-related pain fit orthopedic care.",
    keywords: ["joint", "knee", "back", "bone", "fracture", "sprain", "shoulder", "neck"],
    path: "/patient/book-appointment?specialization=Orthopedics"
  },
  {
    specialty: "Pediatrics",
    reason: "Symptoms for babies, children, and teenagers should usually start with a pediatrician.",
    keywords: ["child", "baby", "kid", "infant", "pediatric", "teen"],
    path: "/patient/book-appointment?specialization=Pediatrics"
  },
  {
    specialty: "General Medicine",
    reason: "Fever, cough, cold, headache, stomach upset, fatigue, and mixed symptoms are good first-visit cases for general medicine.",
    keywords: ["fever", "cough", "cold", "sore throat", "headache", "fatigue", "vomit", "nausea", "stomach", "diarrhea", "body ache"],
    path: "/patient/book-appointment?specialization=General%20Medicine"
  }
];

const emergencyKeywords = [
  "severe chest pain",
  "cannot breathe",
  "trouble breathing",
  "fainting",
  "stroke",
  "one side weakness",
  "heavy bleeding",
  "seizure",
  "unconscious",
  "suicidal"
];

const quickPrompts = [
  "What can I do here?",
  "Which doctor for fever and cough?",
  "Which doctor for chest discomfort?",
  "How do I book an appointment?"
];

const GUIDE_STORAGE_PREFIX = "medflowGuideMessages:";
const GUIDE_INTRO_STORAGE_PREFIX = "medflowGuideIntroDismissed:";
const MAX_STORED_MESSAGES = 30;

function storageKeyForAuth(auth: AuthState | null): string | null {
  if (!auth?.userId) {
    return null;
  }

  return GUIDE_STORAGE_PREFIX + auth.userId;
}

function introStorageKeyForAuth(auth: AuthState | null): string | null {
  if (!auth?.userId) {
    return null;
  }

  return GUIDE_INTRO_STORAGE_PREFIX + auth.userId;
}

function isStoredGuideMessage(value: unknown): value is GuideMessage {
  if (!value || typeof value !== "object") {
    return false;
  }

  const message = value as Partial<GuideMessage>;
  return typeof message.id === "number" && typeof message.text === "string" && (message.owner === "bot" || message.owner === "user");
}

function readStoredMessages(auth: AuthState | null): GuideMessage[] {
  const storageKey = storageKeyForAuth(auth);
  if (!storageKey) {
    return [welcomeMessage(auth)];
  }

  try {
    const stored = window.localStorage.getItem(storageKey);
    if (!stored) {
      return [welcomeMessage(auth)];
    }

    const parsed = JSON.parse(stored) as unknown;
    if (!Array.isArray(parsed)) {
      return [welcomeMessage(auth)];
    }

    const messages = parsed.filter(isStoredGuideMessage);
    return messages.length > 0 ? messages : [welcomeMessage(auth)];
  } catch {
    return [welcomeMessage(auth)];
  }
}

function saveStoredMessages(storageKey: string | null, messages: GuideMessage[]) {
  if (!storageKey) {
    return;
  }

  try {
    window.localStorage.setItem(storageKey, JSON.stringify(messages.slice(-MAX_STORED_MESSAGES)));
  } catch {
    window.localStorage.removeItem(storageKey);
  }
}

function clearStoredGuideMessages() {
  try {
    Object.keys(window.localStorage)
      .filter((key) => key.startsWith(GUIDE_STORAGE_PREFIX))
      .forEach((key) => window.localStorage.removeItem(key));
  } catch {
    return;
  }
}

function readIntroDismissed(auth: AuthState | null): boolean {
  const storageKey = introStorageKeyForAuth(auth);
  if (!storageKey) {
    return false;
  }

  return window.localStorage.getItem(storageKey) === "true";
}

function saveIntroDismissed(auth: AuthState | null) {
  const storageKey = introStorageKeyForAuth(auth);
  if (storageKey) {
    window.localStorage.setItem(storageKey, "true");
  }
}

function displayNameFromAuth(auth: AuthState | null): string {
  if (!auth?.email) {
    return "there";
  }

  const localPart = auth.email.split("@")[0];
  const namePart = localPart
    .split(/[._-]/)
    .find((part) => part && !["patient", "doctor", "admin", "local", "demo"].includes(part.toLowerCase()));

  if (!namePart) {
    return "there";
  }

  return namePart.charAt(0).toUpperCase() + namePart.slice(1);
}

function introForAuth(auth: AuthState | null): GuideIntro {
  if (auth?.role === "PATIENT") {
    return {
      title: "Hi " + displayNameFromAuth(auth) + ", I am MedFlow Guide.",
      body: "Click me for features, available slots, and doctor suggestions."
    };
  }

  if (auth?.role === "DOCTOR") {
    return {
      title: "Hi " + displayNameFromAuth(auth) + ", I am your clinic guide.",
      body: "Click me for patient records, encounters, and symptom support."
    };
  }

  if (auth?.role === "ADMIN") {
    return {
      title: "Hi " + displayNameFromAuth(auth) + ", I am your admin guide.",
      body: "Click me for doctor setup and audit log shortcuts."
    };
  }

  return {
    title: "Hi there, I am MedFlow Guide.",
    body: "Click me to learn features and choose a starting doctor."
  };
}

function welcomeMessage(auth: AuthState | null): GuideMessage {
  return {
    id: 1,
    owner: "bot",
    text: greetingForRole(auth?.role),
    actions: actionsForRole(auth?.role)
  };
}

function greetingForRole(role: Role | undefined): string {
  if (role === "PATIENT") {
    return "Hi, I am the MedFlow Guide. I can help you find features, book a slot, or choose a starting doctor based on symptoms.";
  }

  if (role === "DOCTOR") {
    return "Hi, I am the MedFlow Guide. I can point you to appointments, patient records, encounters, and the symptom pattern classifier.";
  }

  if (role === "ADMIN") {
    return "Hi, I am the MedFlow Guide. I can help you find doctor creation and audit log features.";
  }

  return "Hi, I am the MedFlow Guide. I can explain MedFlow features and suggest which type of doctor a patient may start with.";
}

function actionsForRole(role: Role | undefined): GuideAction[] {
  if (role === "PATIENT") {
    return [
      { label: "Book appointment", path: "/patient/book-appointment" },
      { label: "Care timeline", path: "/patient/timeline" },
      { label: "Find doctors", path: "/patient/doctors" }
    ];
  }

  if (role === "DOCTOR") {
    return [
      { label: "Appointments", path: "/doctor/appointments" },
      { label: "Patient records", path: "/doctor/patients" },
      { label: "Symptom classifier", path: "/doctor/symptom-classifier" }
    ];
  }

  if (role === "ADMIN") {
    return [
      { label: "Create doctor", path: "/admin/doctors" },
      { label: "Audit logs", path: "/admin/audit-logs" }
    ];
  }

  return [
    { label: "Login", path: "/login" },
    { label: "Register", path: "/register" }
  ];
}

function isEmergencyQuestion(normalizedQuestion: string): boolean {
  return emergencyKeywords.some((keyword) => normalizedQuestion.includes(keyword));
}

function findSpecialty(question: string): SpecialtyMatch {
  const normalizedQuestion = question.toLowerCase();
  const matchedSpecialty = specialtyMatches.find((match) => {
    return match.keywords.some((keyword) => normalizedQuestion.includes(keyword));
  });

  return matchedSpecialty || specialtyMatches[specialtyMatches.length - 1];
}

function buildBotReply(question: string, role: Role | undefined, isAuthenticated: boolean): GuideMessage {
  const normalizedQuestion = question.toLowerCase();

  if (isEmergencyQuestion(normalizedQuestion)) {
    return {
      id: Date.now(),
      owner: "bot",
      text: "These symptoms may need urgent care. Please contact local emergency services or visit the nearest emergency department. MedFlow booking is useful for routine clinic visits, not emergencies."
    };
  }

  if (normalizedQuestion.includes("feature") || normalizedQuestion.includes("what can") || normalizedQuestion.includes("help")) {
    return featureReply(role);
  }

  if (normalizedQuestion.includes("book") || normalizedQuestion.includes("appointment") || normalizedQuestion.includes("slot")) {
    return {
      id: Date.now(),
      owner: "bot",
      text: isAuthenticated && role === "PATIENT"
        ? "Go to Book Appointment, choose a doctor and date, then pick one of the available 30-minute slots. Cancelled visits stay in history but free the slot again."
        : "Patients can register or login, then use Book Appointment to choose a doctor, date, and available 30-minute slot.",
      actions: isAuthenticated && role === "PATIENT"
        ? [{ label: "Book appointment", path: "/patient/book-appointment" }]
        : [{ label: "Create patient account", path: "/register" }]
    };
  }

  if (normalizedQuestion.includes("symptom") || normalizedQuestion.includes("doctor") || normalizedQuestion.includes("consult")) {
    return symptomReply(question, role, isAuthenticated);
  }

  return {
    id: Date.now(),
    owner: "bot",
    text: "I can help with MedFlow features, booking steps, patient records, prescriptions, and basic symptom-to-specialization guidance. Try asking: which doctor for fever and cough?",
    actions: actionsForRole(role)
  };
}

function featureReply(role: Role | undefined): GuideMessage {
  if (role === "PATIENT") {
    return {
      id: Date.now(),
      owner: "bot",
      text: "As a patient, you can browse doctors, book available slots, cancel scheduled appointments, view your care timeline, read encounter history, and check prescriptions.",
      actions: actionsForRole(role)
    };
  }

  if (role === "DOCTOR") {
    return {
      id: Date.now(),
      owner: "bot",
      text: "As a doctor, you can view assigned appointments, create encounters, add diagnoses and prescriptions, review connected patient records, and use the symptom pattern classifier for educational support.",
      actions: actionsForRole(role)
    };
  }

  if (role === "ADMIN") {
    return {
      id: Date.now(),
      owner: "bot",
      text: "As an admin, you can create doctor accounts and review audit logs for important system actions.",
      actions: actionsForRole(role)
    };
  }

  return {
    id: Date.now(),
    owner: "bot",
    text: "MedFlow supports patient registration, login, doctor browsing, appointment booking, appointment history, encounters, prescriptions, doctor workflows, and admin audit tracking.",
    actions: actionsForRole(role)
  };
}

function symptomReply(question: string, role: Role | undefined, isAuthenticated: boolean): GuideMessage {
  const match = findSpecialty(question);
  let actions: GuideAction[];

  if (isAuthenticated && role === "PATIENT") {
    actions = [
      { label: "Book " + match.specialty, path: match.path },
      { label: "View doctors", path: "/patient/doctors" }
    ];
  } else if (isAuthenticated && role === "DOCTOR") {
    actions = [
      { label: "Symptom classifier", path: "/doctor/symptom-classifier" },
      { label: "Patient records", path: "/doctor/patients" }
    ];
  } else if (isAuthenticated) {
    actions = actionsForRole(role);
  } else {
    actions = [
      { label: "Register as patient", path: "/register" },
      { label: "Login", path: "/login" }
    ];
  }

  return {
    id: Date.now(),
    owner: "bot",
    text: "For these symptoms, a good starting point is " + match.specialty + ". " + match.reason + " This is guidance only, not a diagnosis.",
    actions
  };
}

async function playGuideChime() {
  const audioWindow = window as Window & { webkitAudioContext?: typeof AudioContext };
  const AudioContextConstructor = window.AudioContext || audioWindow.webkitAudioContext;
  if (!AudioContextConstructor) {
    return;
  }

  const audioContext = new AudioContextConstructor();
  if (audioContext.state === "suspended") {
    await audioContext.resume();
  }

  const now = audioContext.currentTime;
  const masterGain = audioContext.createGain();
  masterGain.gain.setValueAtTime(0.0001, now);
  masterGain.gain.exponentialRampToValueAtTime(0.08, now + 0.03);
  masterGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.55);
  masterGain.connect(audioContext.destination);

  [523.25, 659.25, 783.99].forEach((frequency, index) => {
    const oscillator = audioContext.createOscillator();
    const noteGain = audioContext.createGain();
    const startTime = now + index * 0.11;
    const endTime = startTime + 0.28;

    oscillator.type = "sine";
    oscillator.frequency.setValueAtTime(frequency, startTime);
    noteGain.gain.setValueAtTime(0.0001, startTime);
    noteGain.gain.exponentialRampToValueAtTime(0.16, startTime + 0.025);
    noteGain.gain.exponentialRampToValueAtTime(0.0001, endTime);

    oscillator.connect(noteGain);
    noteGain.connect(masterGain);
    oscillator.start(startTime);
    oscillator.stop(endTime + 0.02);
  });

  window.setTimeout(() => {
    void audioContext.close();
  }, 700);
}

export function MedFlowGuideBot() {
  const { auth, isAuthenticated } = useAuth();
  const storageKey = storageKeyForAuth(auth);
  const playedIntroSoundRef = useRef(false);
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState<GuideMessage[]>(() => readStoredMessages(auth));
  const [introDismissed, setIntroDismissed] = useState(() => readIntroDismissed(auth));

  useEffect(() => {
    playedIntroSoundRef.current = false;

    if (!auth) {
      clearStoredGuideMessages();
      setMessages([welcomeMessage(null)]);
      setIntroDismissed(false);
      return;
    }

    setMessages(readStoredMessages(auth));
    setIntroDismissed(readIntroDismissed(auth));
  }, [auth?.role, auth?.userId]);

  useEffect(() => {
    if (open || introDismissed || playedIntroSoundRef.current) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      playedIntroSoundRef.current = true;
      void playGuideChime().catch(() => {
        playedIntroSoundRef.current = false;
      });
    }, 700);

    return () => window.clearTimeout(timeoutId);
  }, [introDismissed, open]);

  const panelTitle = useMemo(() => {
    if (auth?.role === "DOCTOR") {
      return "Doctor Guide";
    }

    if (auth?.role === "ADMIN") {
      return "Admin Guide";
    }

    return "MedFlow Guide";
  }, [auth?.role]);

  const intro = useMemo(() => introForAuth(auth), [auth]);

  function playChimeAfterUserAction() {
    if (playedIntroSoundRef.current) {
      return;
    }

    playedIntroSoundRef.current = true;
    void playGuideChime().catch(() => {
      playedIntroSoundRef.current = false;
    });
  }

  function openGuide() {
    playChimeAfterUserAction();
    saveIntroDismissed(auth);
    setIntroDismissed(true);
    setOpen(true);
  }

  function toggleGuide() {
    if (!open) {
      openGuide();
      return;
    }

    setOpen(false);
  }

  function askGuide(question: string) {
    const trimmedQuestion = question.trim();
    if (!trimmedQuestion) {
      return;
    }

    const userMessage: GuideMessage = {
      id: Date.now() - 1,
      owner: "user",
      text: trimmedQuestion
    };

    const botMessage = buildBotReply(trimmedQuestion, auth?.role, isAuthenticated);
    setMessages((current) => {
      const nextMessages = current.concat(userMessage, botMessage).slice(-MAX_STORED_MESSAGES);
      saveStoredMessages(storageKey, nextMessages);
      return nextMessages;
    });
    setInput("");
    setOpen(true);
  }

  return (
    <aside className={"guide-bot" + (open ? " guide-bot-open" : "")} aria-label="MedFlow guide assistant">
      {open ? (
        <section className="guide-panel">
          <div className="guide-header">
            <div>
              <span className="eyebrow">Helper bot</span>
              <h2>{panelTitle}</h2>
            </div>
            <button type="button" className="guide-icon-button" onClick={() => setOpen(false)} aria-label="Close guide">
              x
            </button>
          </div>

          <div className="guide-messages">
            {messages.map((message) => (
              <article key={message.id} className={"guide-message guide-message-" + message.owner}>
                <p>{message.text}</p>
                {message.actions && message.actions.length > 0 ? (
                  <div className="guide-actions">
                    {message.actions.map((action) => (
                      <Link key={action.path + action.label} to={action.path} onClick={() => setOpen(false)}>
                        {action.label}
                      </Link>
                    ))}
                  </div>
                ) : null}
              </article>
            ))}
          </div>

          <div className="guide-prompts">
            {quickPrompts.map((prompt) => (
              <button key={prompt} type="button" onClick={() => askGuide(prompt)}>
                {prompt}
              </button>
            ))}
          </div>

          <form className="guide-form" onSubmit={(event) => { event.preventDefault(); askGuide(input); }}>
            <input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Ask about features or symptoms"
              aria-label="Ask MedFlow guide"
            />
            <button type="submit">Send</button>
          </form>
        </section>
      ) : null}

      {!open && !introDismissed ? (
        <button type="button" className="guide-intro-bubble" onClick={openGuide}>
          <strong>{intro.title}</strong>
          <span>{intro.body}</span>
        </button>
      ) : null}

      <button type="button" className="guide-launcher" onClick={toggleGuide} aria-label="Open MedFlow guide">
        <span className="guide-launcher-mark" aria-hidden="true">
          <span className="guide-companion-core">
            <span className="guide-companion-eye guide-companion-eye-left" />
            <span className="guide-companion-eye guide-companion-eye-right" />
          </span>
          <span className="guide-companion-spark guide-companion-spark-one" />
          <span className="guide-companion-spark guide-companion-spark-two" />
        </span>
        <span className="sr-only">Open MedFlow guide</span>
      </button>
    </aside>
  );
}
