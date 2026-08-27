from pathlib import Path
from typing import Any

import joblib
import pandas as pd

from app.schemas import SymptomPredictionRequest

DISCLAIMER = (
    "Educational prediction only. Not a medical diagnosis. "
    "Final clinical decisions must be made by a qualified clinician."
)

SUPPORTED_PATTERNS = {
    "RESPIRATORY_PATTERN",
    "VIRAL_LIKE_PATTERN",
    "ALLERGY_LIKE_PATTERN",
    "GASTROINTESTINAL_PATTERN",
    "MIGRAINE_LIKE_PATTERN",
    "MUSCULOSKELETAL_PATTERN",
    "GENERAL_UNSPECIFIED_PATTERN",
}

SUPPORTED_CAUSES = {
    "COMMON_COLD_LIKE",
    "BRONCHITIS_AIRWAY_IRRITATION_LIKE",
    "FLU_LIKE_VIRAL_ILLNESS",
    "ALLERGIC_RHINITIS_LIKE",
    "ACUTE_GASTROENTERITIS_LIKE",
    "FOOD_RELATED_STOMACH_UPSET_LIKE",
    "MIGRAINE_LIKE_HEADACHE",
    "MUSCLE_STRAIN_LIKE",
    "JOINT_INFLAMMATION_LIKE",
    "NON_SPECIFIC_SYMPTOM_CLUSTER",
}

MODEL_PATH = Path(__file__).resolve().parents[1] / "model" / "symptom_classifier.joblib"
MODEL_VERSION = "synthetic-cause-logreg-v3"
MODEL_NAME = "MedFlow Educational Symptom Cause Model"
MODEL_TYPE = "Two scikit-learn Logistic Regression pipelines"
SERVER_MODE = "FastAPI model server loading a saved pre-trained joblib artifact"

FEATURE_COLUMNS = [
    "fever",
    "cough",
    "sore_throat",
    "runny_nose",
    "sneezing",
    "headache",
    "fatigue",
    "nausea",
    "vomiting",
    "abdominal_pain",
    "diarrhea",
    "chest_discomfort",
    "shortness_of_breath",
    "body_ache",
    "joint_pain",
    "dizziness",
    "light_sensitivity",
    "symptom_duration_days",
    "age_group",
]

FEATURE_LABELS = {
    "fever": "Fever",
    "cough": "Cough",
    "sore_throat": "Sore throat",
    "runny_nose": "Runny nose",
    "sneezing": "Sneezing",
    "headache": "Headache",
    "fatigue": "Fatigue",
    "nausea": "Nausea",
    "vomiting": "Vomiting",
    "abdominal_pain": "Abdominal pain",
    "diarrhea": "Diarrhea",
    "chest_discomfort": "Chest discomfort",
    "shortness_of_breath": "Shortness of breath",
    "body_ache": "Body ache",
    "joint_pain": "Joint pain",
    "dizziness": "Dizziness",
    "light_sensitivity": "Light sensitivity",
    "symptom_duration_days": "Symptom duration",
    "age_group": "Age group",
}

CAUSE_DETAILS = {
    "COMMON_COLD_LIKE": {
        "title": "Common cold or upper respiratory infection-like cause",
        "reason": "Cough, sore throat, runny nose, and a short symptom duration are strongest in this trained cause profile.",
        "evidenceFeatures": ["cough", "sore_throat", "runny_nose", "sneezing", "symptom_duration_days"],
        "uncertaintyNotes": [
            "The model cannot identify a specific virus from symptoms alone.",
            "Fever, breathing symptoms, vitals, and examination can change the clinical interpretation.",
        ],
        "nextSteps": [
            "Review fever pattern, cough severity, and exposure history.",
            "Check for chest discomfort, breathlessness, persistent fever, or worsening symptoms.",
        ],
        "questions": [
            "When did the cough and throat symptoms start?",
            "Is there fever, breathing difficulty, chest discomfort, or worsening cough?",
            "Any recent sick contacts or similar symptoms at home?",
        ],
    },
    "BRONCHITIS_AIRWAY_IRRITATION_LIKE": {
        "title": "Bronchitis or airway irritation-like cause",
        "reason": "Persistent cough with chest discomfort or breathing symptoms fits this cause profile more than a simple cold profile.",
        "evidenceFeatures": ["cough", "chest_discomfort", "shortness_of_breath", "fatigue", "symptom_duration_days"],
        "uncertaintyNotes": [
            "The model does not listen to breath sounds or review oxygen saturation.",
            "Respiratory causes need clinical review when chest or breathing symptoms are present.",
        ],
        "nextSteps": [
            "Check oxygen saturation, respiratory rate, temperature, and chest findings.",
            "Ask about asthma, smoking exposure, dust exposure, and symptom progression.",
        ],
        "questions": [
            "Is the cough dry or productive, and how many days has it continued?",
            "Is there wheezing, chest tightness, or shortness of breath?",
            "Any asthma history, smoke exposure, or recent respiratory infection?",
        ],
    },
    "FLU_LIKE_VIRAL_ILLNESS": {
        "title": "Flu-like viral illness cause",
        "reason": "Fever, fatigue, body ache, headache, and throat symptoms together strongly support this trained cause profile.",
        "evidenceFeatures": ["fever", "fatigue", "body_ache", "headache", "sore_throat", "cough"],
        "uncertaintyNotes": [
            "Symptoms can overlap with several viral and bacterial illnesses.",
            "Testing, vitals, local outbreaks, and examination are needed before making a medical diagnosis.",
        ],
        "nextSteps": [
            "Review fever duration, hydration, breathing symptoms, and risk factors.",
            "Consider whether local testing or clinician-directed treatment advice is needed.",
        ],
        "questions": [
            "When did fever, fatigue, and body ache begin?",
            "Are symptoms improving, persistent, or worsening?",
            "Any cough, sore throat, sick contacts, or recent travel?",
        ],
    },
    "ALLERGIC_RHINITIS_LIKE": {
        "title": "Allergic rhinitis-like cause",
        "reason": "Sneezing and runny nose with little fever influence this trained cause profile.",
        "evidenceFeatures": ["sneezing", "runny_nose", "cough", "headache", "symptom_duration_days"],
        "uncertaintyNotes": [
            "The model cannot confirm an allergen or rule out infection.",
            "Fever, body ache, and worsening symptoms would reduce confidence in a purely allergy-like explanation.",
        ],
        "nextSteps": [
            "Ask about dust, pollen, pets, weather changes, and seasonal recurrence.",
            "Review whether symptoms are mainly nasal or include fever and body ache.",
        ],
        "questions": [
            "Are symptoms worse around dust, pollen, pets, or weather changes?",
            "Is there fever or body ache, or mainly sneezing and runny nose?",
            "Has this happened during similar seasons before?",
        ],
    },
    "ACUTE_GASTROENTERITIS_LIKE": {
        "title": "Acute gastroenteritis-like cause",
        "reason": "Nausea, vomiting, abdominal pain, diarrhea, and dizziness are strong signals in this trained cause profile.",
        "evidenceFeatures": ["nausea", "vomiting", "abdominal_pain", "diarrhea", "dizziness", "fever"],
        "uncertaintyNotes": [
            "The model cannot identify the organism or severity of dehydration.",
            "Severe pain, blood in stool, reduced urine, or persistent fever need careful clinical assessment.",
        ],
        "nextSteps": [
            "Review hydration, urine output, number of episodes, fever, and stool changes.",
            "Check for red flags such as blood, severe abdominal pain, or dizziness.",
        ],
        "questions": [
            "How many vomiting or diarrhea episodes occurred today?",
            "Any outside food, unsafe water exposure, or similar illness in family members?",
            "Is there dizziness, reduced urine, blood in stool, or severe abdominal pain?",
        ],
    },
    "FOOD_RELATED_STOMACH_UPSET_LIKE": {
        "title": "Food-related stomach upset-like cause",
        "reason": "Short-duration nausea and abdominal pain with less fever or diarrhea fits this trained cause profile.",
        "evidenceFeatures": ["nausea", "abdominal_pain", "vomiting", "diarrhea", "symptom_duration_days"],
        "uncertaintyNotes": [
            "Symptoms alone cannot confirm food poisoning or rule out other abdominal causes.",
            "Pain location, severity, fever, hydration, and examination remain important.",
        ],
        "nextSteps": [
            "Ask about recent meals, water exposure, medications, and symptom timing.",
            "Review abdominal pain severity, fever, hydration, and worsening symptoms.",
        ],
        "questions": [
            "What food or drink was taken before symptoms started?",
            "Did nausea or abdominal pain start suddenly after a meal?",
            "Is there fever, severe pain, repeated vomiting, or dehydration?",
        ],
    },
    "MIGRAINE_LIKE_HEADACHE": {
        "title": "Migraine-like headache cause",
        "reason": "Headache with nausea, dizziness, and light sensitivity is the key trained profile for this cause.",
        "evidenceFeatures": ["headache", "light_sensitivity", "nausea", "vomiting", "dizziness"],
        "uncertaintyNotes": [
            "The model cannot assess neurologic examination or headache danger signs.",
            "Sudden severe headache, weakness, confusion, or visual changes need urgent clinical attention.",
        ],
        "nextSteps": [
            "Review previous headache history, trigger pattern, and neurologic red flags.",
            "Ask about sleep, hydration, stress, screen exposure, and medication use.",
        ],
        "questions": [
            "Has the patient had similar headaches before?",
            "Is there sudden severe onset, weakness, confusion, or visual change?",
            "Any sleep loss, stress, dehydration, screen exposure, or known trigger?",
        ],
    },
    "MUSCLE_STRAIN_LIKE": {
        "title": "Muscle strain or exertion-related cause",
        "reason": "Body ache with limited systemic symptoms fits this trained muscle strain-like profile.",
        "evidenceFeatures": ["body_ache", "joint_pain", "fatigue", "symptom_duration_days"],
        "uncertaintyNotes": [
            "The model does not know injury history, pain location, swelling, or examination findings.",
            "Fever, weakness, rash, or worsening pain can suggest a broader clinical problem.",
        ],
        "nextSteps": [
            "Ask about recent exercise, injury, posture, lifting, or unusual activity.",
            "Review pain location, movement limitation, swelling, fever, and weakness.",
        ],
        "questions": [
            "Was there recent exercise, injury, strain, or heavy lifting?",
            "Is pain localized or generalized?",
            "Any fever, swelling, weakness, rash, or worsening pain?",
        ],
    },
    "JOINT_INFLAMMATION_LIKE": {
        "title": "Joint inflammation-like cause",
        "reason": "Joint pain, longer duration, fatigue, and occasional fever guide this trained cause profile.",
        "evidenceFeatures": ["joint_pain", "body_ache", "fatigue", "fever", "symptom_duration_days"],
        "uncertaintyNotes": [
            "The model cannot assess joint swelling, stiffness, injury, rash, or lab findings.",
            "Persistent or worsening joint symptoms should be interpreted with examination.",
        ],
        "nextSteps": [
            "Ask about swelling, morning stiffness, injury, rash, fever, and number of joints involved.",
            "Review duration and whether pain is worsening or limiting daily activity.",
        ],
        "questions": [
            "Which joints are painful, and are they swollen or stiff?",
            "Is stiffness worse in the morning or after rest?",
            "Any fever, rash, injury, or worsening limitation?",
        ],
    },
    "NON_SPECIFIC_SYMPTOM_CLUSTER": {
        "title": "Non-specific symptom cluster",
        "reason": "The submitted symptoms do not strongly match one focused trained cause profile.",
        "evidenceFeatures": ["fatigue", "headache", "dizziness", "symptom_duration_days"],
        "uncertaintyNotes": [
            "More history and examination are more important than the current symptom checklist.",
            "Binary symptom inputs do not capture severity, timing, progression, or vital signs.",
        ],
        "nextSteps": [
            "Clarify the main concern, first symptom, progression, exposures, and medications.",
            "Check for fever, breathing issues, severe pain, dehydration, confusion, or weakness.",
        ],
        "questions": [
            "What is the main symptom worrying the patient today?",
            "Which symptom appeared first, and how has it changed?",
            "Any fever, worsening pain, breathing issue, dehydration, or new medication use?",
        ],
    },
}

DEFAULT_CAUSE_BY_PATTERN = {
    "RESPIRATORY_PATTERN": "COMMON_COLD_LIKE",
    "VIRAL_LIKE_PATTERN": "FLU_LIKE_VIRAL_ILLNESS",
    "ALLERGY_LIKE_PATTERN": "ALLERGIC_RHINITIS_LIKE",
    "GASTROINTESTINAL_PATTERN": "ACUTE_GASTROENTERITIS_LIKE",
    "MIGRAINE_LIKE_PATTERN": "MIGRAINE_LIKE_HEADACHE",
    "MUSCULOSKELETAL_PATTERN": "MUSCLE_STRAIN_LIKE",
    "GENERAL_UNSPECIFIED_PATTERN": "NON_SPECIFIC_SYMPTOM_CLUSTER",
}


class SymptomPatternPredictor:
    def __init__(self, model_path: Path = MODEL_PATH) -> None:
        if not model_path.exists():
            raise RuntimeError(
                f"Model file not found at {model_path}. Run generate_data.py and train.py first."
            )

        artifact = joblib.load(model_path)
        if isinstance(artifact, dict) and "patternModel" in artifact:
            self.pattern_model = artifact["patternModel"]
            self.cause_model = artifact.get("causeModel")
            self.model_name = str(artifact.get("modelName", MODEL_NAME))
            self.model_type = str(artifact.get("modelType", MODEL_TYPE))
            self.model_version = str(artifact.get("modelVersion", MODEL_VERSION))
            self.server_mode = str(artifact.get("serverMode", SERVER_MODE))
            self.training_rows = artifact.get("trainingRows")
            self.pattern_accuracy = artifact.get("patternAccuracy")
            self.cause_accuracy = artifact.get("causeAccuracy")
            self.supported_patterns = set(artifact.get("supportedPatterns", SUPPORTED_PATTERNS))
            self.supported_causes = set(artifact.get("supportedCauses", SUPPORTED_CAUSES))
        else:
            self.pattern_model = artifact
            self.cause_model = None
            self.model_name = MODEL_NAME
            self.model_type = "scikit-learn Logistic Regression pipeline"
            self.model_version = "synthetic-logreg-v2"
            self.server_mode = SERVER_MODE
            self.training_rows = None
            self.pattern_accuracy = None
            self.cause_accuracy = None
            self.supported_patterns = SUPPORTED_PATTERNS
            self.supported_causes = SUPPORTED_CAUSES

    def predict(self, request: SymptomPredictionRequest) -> dict[str, Any]:
        features = self._to_features(request)
        frame = pd.DataFrame([features], columns=FEATURE_COLUMNS)

        pattern_ranked = self._rank_predictions(self.pattern_model, frame, 3)
        predicted_pattern = pattern_ranked[0][0]
        pattern_confidence = pattern_ranked[0][1]
        self._validate_pattern(predicted_pattern)

        cause_ranked = self._rank_causes(frame, predicted_pattern, pattern_confidence)
        predicted_cause = cause_ranked[0][0]
        cause_confidence = cause_ranked[0][1]
        self._validate_cause(predicted_cause)

        confidence_level = self._confidence_level(pattern_confidence)
        cause_confidence_level = self._confidence_level(cause_confidence)
        factors = self._top_factors(frame, predicted_pattern)

        return {
            "predictedPattern": predicted_pattern,
            "confidence": round(pattern_confidence, 4),
            "alternatives": [
                {
                    "pattern": pattern,
                    "confidence": round(confidence, 4),
                }
                for pattern, confidence in pattern_ranked[1:]
            ],
            "likelyCause": self._likely_cause(
                predicted_cause,
                cause_confidence,
                cause_confidence_level,
                request,
            ),
            "causeAlternatives": [
                {
                    "cause": cause,
                    "confidence": round(confidence, 4),
                }
                for cause, confidence in cause_ranked[1:]
            ],
            "modelVersion": self.model_version,
            "confidenceLevel": confidence_level,
            "contributingFactors": factors,
            "safetyFlags": self._safety_flags(request),
            "possibleCauses": self._possible_causes(predicted_cause, cause_ranked),
            "suggestedDoctorQuestions": self._suggested_questions(predicted_cause),
            "reviewPriority": self._review_priority(request, confidence_level),
            "reviewMessage": self._review_message(request, confidence_level),
            "disclaimer": DISCLAIMER,
        }

    def model_info(self) -> dict[str, Any]:
        pattern_classes = self._classes(self.pattern_model)
        cause_classes = self._classes(self.cause_model) if self.cause_model is not None else []
        metrics_text = self._metrics_text()

        return {
            "modelName": self.model_name,
            "modelType": self.model_type,
            "modelVersion": self.model_version,
            "serverMode": self.server_mode,
            "featureCount": len(FEATURE_COLUMNS),
            "patternCount": len(pattern_classes),
            "supportedPatterns": pattern_classes,
            "trainingData": metrics_text,
            "explanationMethod": (
                "Pattern model uses class-specific logistic regression contributions; "
                "cause model returns ranked likely-cause probabilities"
            ),
            "disclaimer": DISCLAIMER,
        }

    def _to_features(self, request: SymptomPredictionRequest) -> dict[str, object]:
        return {
            "fever": request.fever,
            "cough": request.cough,
            "sore_throat": request.soreThroat,
            "runny_nose": request.runnyNose,
            "sneezing": request.sneezing,
            "headache": request.headache,
            "fatigue": request.fatigue,
            "nausea": request.nausea,
            "vomiting": request.vomiting,
            "abdominal_pain": request.abdominalPain,
            "diarrhea": request.diarrhea,
            "chest_discomfort": request.chestDiscomfort,
            "shortness_of_breath": request.shortnessOfBreath,
            "body_ache": request.bodyAche,
            "joint_pain": request.jointPain,
            "dizziness": request.dizziness,
            "light_sensitivity": request.lightSensitivity,
            "symptom_duration_days": request.symptomDurationDays,
            "age_group": request.ageGroup.value,
        }

    def _rank_predictions(self, model: Any, frame: pd.DataFrame, limit: int) -> list[tuple[str, float]]:
        if model is None:
            return []

        if not hasattr(model, "predict_proba"):
            return [(str(model.predict(frame)[0]), 1.0)]

        probabilities = model.predict_proba(frame)[0]
        classes = [str(item) for item in model.classes_]
        ranked = sorted(
            zip(classes, probabilities, strict=True),
            key=lambda item: item[1],
            reverse=True,
        )[:limit]
        return [(label, float(confidence)) for label, confidence in ranked]

    def _rank_causes(
        self,
        frame: pd.DataFrame,
        predicted_pattern: str,
        pattern_confidence: float,
    ) -> list[tuple[str, float]]:
        if self.cause_model is None:
            return [(DEFAULT_CAUSE_BY_PATTERN[predicted_pattern], pattern_confidence)]

        return self._rank_predictions(self.cause_model, frame, 3)

    def _classes(self, model: Any) -> list[str]:
        if model is None:
            return []
        return [str(item) for item in getattr(model, "classes_", [])]

    def _metrics_text(self) -> str:
        rows = self.training_rows if self.training_rows is not None else "unknown"
        if self.pattern_accuracy is None or self.cause_accuracy is None:
            return f"Synthetic MedFlow symptom dataset generated locally for educational use ({rows} rows)"

        return (
            "Synthetic MedFlow cause-profile dataset generated locally for educational use "
            f"({rows} rows; pattern accuracy {self.pattern_accuracy}; cause accuracy {self.cause_accuracy})"
        )

    def _validate_pattern(self, pattern: str) -> None:
        if pattern not in self.supported_patterns:
            raise RuntimeError(f"Unsupported pattern returned by model: {pattern}")

    def _validate_cause(self, cause: str) -> None:
        if cause not in self.supported_causes:
            raise RuntimeError(f"Unsupported cause returned by model: {cause}")

    def _confidence_level(self, confidence: float) -> str:
        if confidence >= 0.75:
            return "HIGH"
        if confidence >= 0.50:
            return "MEDIUM"
        return "LOW"

    def _top_factors(self, frame: pd.DataFrame, predicted_pattern: str) -> list[dict[str, Any]]:
        model = self.pattern_model
        if not hasattr(model, "named_steps"):
            return []

        preprocessor = model.named_steps.get("preprocessor")
        classifier = model.named_steps.get("classifier")
        if preprocessor is None or classifier is None or not hasattr(classifier, "coef_"):
            return []

        transformed = preprocessor.transform(frame)
        feature_names = [str(name) for name in preprocessor.get_feature_names_out()]
        class_index = list(classifier.classes_).index(predicted_pattern)
        contributions = transformed[0] * classifier.coef_[class_index]

        ranked_factors = sorted(
            zip(feature_names, contributions, strict=True),
            key=lambda item: abs(float(item[1])),
            reverse=True,
        )

        factors = []
        for raw_name, contribution in ranked_factors:
            field = self._clean_feature_name(raw_name)
            if field.endswith("_CHILD") or field.endswith("_ADULT"):
                field = "age_group"

            value = self._feature_value(frame, field)
            if value in {"0", "0.0", ""}:
                continue

            factors.append(
                {
                    "field": field,
                    "label": FEATURE_LABELS.get(field, field.replace("_", " ").title()),
                    "value": value,
                    "contribution": round(float(contribution), 4),
                }
            )

            if len(factors) == 5:
                break

        return factors

    def _clean_feature_name(self, raw_name: str) -> str:
        cleaned = raw_name
        if "__" in cleaned:
            cleaned = cleaned.split("__", 1)[1]
        if cleaned.startswith("age_group_"):
            return cleaned
        return cleaned

    def _feature_value(self, frame: pd.DataFrame, field: str) -> str:
        if field == "age_group":
            return str(frame.iloc[0]["age_group"])
        if field == "symptom_duration_days":
            days = int(frame.iloc[0]["symptom_duration_days"])
            return f"{days} day" if days == 1 else f"{days} days"
        if field in frame.columns:
            return "Present" if int(frame.iloc[0][field]) == 1 else "Absent"
        return ""

    def _safety_flags(self, request: SymptomPredictionRequest) -> list[dict[str, str]]:
        flags = []

        if request.shortnessOfBreath == 1 or request.chestDiscomfort == 1:
            flags.append(
                {
                    "code": "BREATHING_OR_CHEST_SYMPTOM",
                    "severity": "REVIEW",
                    "message": "Breathing difficulty or chest discomfort was selected. Review clinically before relying on pattern confidence.",
                }
            )

        if request.vomiting == 1 and request.diarrhea == 1 and request.dizziness == 1:
            flags.append(
                {
                    "code": "DEHYDRATION_CONTEXT",
                    "severity": "REVIEW",
                    "message": "Vomiting, diarrhea, and dizziness together may need careful clinical assessment.",
                }
            )

        if request.ageGroup.value == "OLDER_ADULT" and request.fever == 1 and request.fatigue == 1:
            flags.append(
                {
                    "code": "OLDER_ADULT_FEVER_FATIGUE",
                    "severity": "REVIEW",
                    "message": "Older adult with fever and fatigue should be assessed carefully by the clinician.",
                }
            )

        return flags

    def _possible_causes(
        self,
        predicted_cause: str,
        cause_ranked: list[tuple[str, float]],
    ) -> list[dict[str, str]]:
        selected_causes = [predicted_cause]
        selected_causes.extend(cause for cause, _ in cause_ranked[1:])

        causes = []
        seen = set()
        for cause in selected_causes:
            if cause in seen:
                continue
            seen.add(cause)
            detail = CAUSE_DETAILS.get(cause, CAUSE_DETAILS["NON_SPECIFIC_SYMPTOM_CLUSTER"])
            causes.append(
                {
                    "title": detail["title"],
                    "reason": detail["reason"],
                }
            )
            if len(causes) == 3:
                break

        return causes

    def _suggested_questions(self, predicted_cause: str) -> list[str]:
        return CAUSE_DETAILS.get(
            predicted_cause,
            CAUSE_DETAILS["NON_SPECIFIC_SYMPTOM_CLUSTER"],
        )["questions"]

    def _likely_cause(
        self,
        predicted_cause: str,
        confidence: float,
        confidence_level: str,
        request: SymptomPredictionRequest,
    ) -> dict[str, Any]:
        detail = CAUSE_DETAILS.get(predicted_cause, CAUSE_DETAILS["NON_SPECIFIC_SYMPTOM_CLUSTER"])
        uncertainty_notes = list(detail["uncertaintyNotes"])
        uncertainty_notes.append("This is trained on synthetic educational data, not real patient records.")

        return {
            "code": predicted_cause,
            "title": detail["title"],
            "confidence": round(confidence, 4),
            "confidenceLevel": confidence_level,
            "evidence": self._cause_evidence(request, detail["evidenceFeatures"]),
            "uncertaintyNotes": uncertainty_notes,
            "nextSteps": detail["nextSteps"],
        }

    def _cause_evidence(self, request: SymptomPredictionRequest, evidence_features: list[str]) -> list[str]:
        features = self._to_features(request)
        evidence = []

        for field in evidence_features:
            if field == "symptom_duration_days":
                days = int(features[field])
                evidence.append(f"Symptom duration entered as {days} day" if days == 1 else f"Symptom duration entered as {days} days")
                continue

            if features.get(field) == 1:
                evidence.append(f"{FEATURE_LABELS.get(field, field.replace('_', ' ').title())} selected")

        if not evidence:
            evidence.append("Cause ranking is based on the overall submitted symptom combination.")

        return evidence[:5]

    def _review_priority(self, request: SymptomPredictionRequest, confidence_level: str) -> str:
        if request.shortnessOfBreath == 1 or request.chestDiscomfort == 1:
            return "PRIORITY_REVIEW"
        if request.vomiting == 1 and request.diarrhea == 1 and request.dizziness == 1:
            return "PRIORITY_REVIEW"
        if confidence_level == "LOW":
            return "NEEDS_MORE_INFORMATION"
        return "ROUTINE_CLINICAL_REVIEW"

    def _review_message(self, request: SymptomPredictionRequest, confidence_level: str) -> str:
        if request.shortnessOfBreath == 1 or request.chestDiscomfort == 1:
            return "Review chest or breathing symptoms carefully before using this pattern as decision support."
        if request.vomiting == 1 and request.diarrhea == 1 and request.dizziness == 1:
            return "Review hydration status and symptom severity carefully."
        if confidence_level == "LOW":
            return "The model confidence is low, so collect more history and clinical findings before interpreting the result."
        return "Use this as an educational support signal along with history, examination, and clinician judgment."
