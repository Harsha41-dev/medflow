from fastapi.testclient import TestClient

from app.main import app
from app.predictor import SUPPORTED_PATTERNS

client = TestClient(app)


def valid_payload() -> dict:
    return {
        "fever": 1,
        "cough": 1,
        "soreThroat": 1,
        "runnyNose": 0,
        "sneezing": 0,
        "headache": 1,
        "fatigue": 1,
        "nausea": 0,
        "vomiting": 0,
        "abdominalPain": 0,
        "diarrhea": 0,
        "chestDiscomfort": 0,
        "shortnessOfBreath": 0,
        "bodyAche": 1,
        "jointPain": 0,
        "dizziness": 0,
        "lightSensitivity": 0,
        "symptomDurationDays": 3,
        "ageGroup": "ADULT",
    }


def test_health_returns_up() -> None:
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "UP"}


def test_model_info_exposes_loaded_model_metadata() -> None:
    response = client.get("/model-info")
    body = response.json()

    assert response.status_code == 200
    assert body["modelVersion"] == "synthetic-cause-logreg-v3"
    assert body["serverMode"].startswith("FastAPI model server")
    assert body["featureCount"] == 19
    assert body["patternCount"] == len(SUPPORTED_PATTERNS)
    assert set(body["supportedPatterns"]) == SUPPORTED_PATTERNS
    assert "cause accuracy" in body["trainingData"]
    assert "Not a medical diagnosis" in body["disclaimer"]


def test_valid_prediction_request() -> None:
    response = client.post("/predict", json=valid_payload())
    body = response.json()

    assert response.status_code == 200
    assert body["predictedPattern"] in SUPPORTED_PATTERNS
    assert 0 <= body["confidence"] <= 1
    assert body["modelVersion"] == "synthetic-cause-logreg-v3"
    assert body["confidenceLevel"] in {"LOW", "MEDIUM", "HIGH"}
    assert body["likelyCause"]["code"]
    assert 0 <= body["likelyCause"]["confidence"] <= 1
    assert len(body["likelyCause"]["evidence"]) > 0
    assert len(body["likelyCause"]["uncertaintyNotes"]) > 0
    assert len(body["likelyCause"]["nextSteps"]) > 0
    assert len(body["causeAlternatives"]) > 0
    assert len(body["contributingFactors"]) > 0
    assert "label" in body["contributingFactors"][0]
    assert len(body["possibleCauses"]) > 0
    assert "reason" in body["possibleCauses"][0]
    assert len(body["suggestedDoctorQuestions"]) > 0
    assert body["reviewPriority"] in {
        "ROUTINE_CLINICAL_REVIEW",
        "PRIORITY_REVIEW",
        "NEEDS_MORE_INFORMATION",
    }
    assert len(body["reviewMessage"]) > 0
    assert "Not a medical diagnosis" in body["disclaimer"]


def test_prediction_returns_safety_flags_for_review_symptoms() -> None:
    payload = valid_payload()
    payload["chestDiscomfort"] = 1
    payload["shortnessOfBreath"] = 1

    response = client.post("/predict", json=payload)
    body = response.json()

    assert response.status_code == 200
    assert body["safetyFlags"][0]["code"] == "BREATHING_OR_CHEST_SYMPTOM"
    assert body["safetyFlags"][0]["severity"] == "REVIEW"


def test_prediction_returns_supported_pattern() -> None:
    response = client.post("/predict", json=valid_payload())

    assert response.status_code == 200
    assert response.json()["predictedPattern"] in SUPPORTED_PATTERNS


def test_invalid_binary_symptom_value_is_rejected() -> None:
    payload = valid_payload()
    payload["fever"] = 2

    response = client.post("/predict", json=payload)

    assert response.status_code == 422


def test_invalid_age_group_is_rejected() -> None:
    payload = valid_payload()
    payload["ageGroup"] = "TEEN"

    response = client.post("/predict", json=payload)

    assert response.status_code == 422
