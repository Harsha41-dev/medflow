# MedFlow Symptom Pattern Classifier

This folder contains the small ML service used by MedFlow. It runs separately from the Spring Boot backend as a FastAPI app and loads a saved scikit-learn `joblib` model when the service starts.

The feature is doctor-only in the main MedFlow application. A doctor enters symptoms and basic patient context, Spring Boot sends the request to this service, and the service returns an educational symptom pattern with likely cause categories, confidence values, contributing factors, safety flags, and suggested follow-up questions.

The model is trained only on synthetic data generated inside this folder. It is useful for showing how a backend can integrate with a separate ML service, but it is not clinically validated and must not be treated as a diagnosis system.

## How It Fits Into MedFlow

```text
React doctor page
  -> Spring Boot protected ML endpoint
  -> FastAPI symptom service
  -> scikit-learn model
  -> explanation-style response
```

The React frontend does not call this service directly. Keeping the call behind Spring Boot lets the main backend enforce authentication and doctor-only authorization.

## What The Service Returns

The response is designed to be understandable instead of just returning a raw model label. It includes:

- broad symptom pattern
- likely cause category
- confidence score
- alternative predictions
- symptom factors that influenced the result
- possible causes to consider
- safety flags
- suggested doctor questions
- model metadata

Example pattern labels include `RESPIRATORY_PATTERN`, `CARDIAC_PATTERN`, `SKIN_PATTERN`, and similar broad groups.

## Model Files

The generated model artifact is stored at:

```text
model/symptom_classifier.joblib
```

The current metrics file is stored at:

```text
model/training_metrics.json
```

Current synthetic holdout metrics:

```text
Pattern model accuracy: 0.9020
Cause model accuracy: 0.8830
Training rows: 5000
Cause labels: 10
```

These numbers describe performance on generated synthetic data only.

## Main Files

- `generate_data.py` creates the synthetic training dataset.
- `train.py` trains the scikit-learn models and saves the artifact.
- `app/main.py` exposes the FastAPI endpoints.
- `app/predictor.py` loads the model and builds the prediction response.
- `app/schemas.py` defines request and response models.
- `tests/test_app.py` checks the important service behavior.

## Local Development

The service can be run locally on port `8000`.

```bash
pip install -r requirements.txt
python generate_data.py
python train.py
uvicorn app.main:app --reload --port 8000
```

Useful endpoints:

```text
GET  /health
GET  /model-info
POST /predict
```

Tests:

```bash
pytest -q
```

## Safety Note

This service is an educational ML integration for a software engineering project. It does not provide medical advice, diagnosis, treatment decisions, or emergency triage.
