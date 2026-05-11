import json
from pathlib import Path

import joblib
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder

RANDOM_STATE = 42

DATA_PATH = Path(__file__).parent / "data" / "synthetic_symptoms.csv"
MODEL_PATH = Path(__file__).parent / "model" / "symptom_classifier.joblib"
METRICS_PATH = Path(__file__).parent / "model" / "training_metrics.json"

MODEL_VERSION = "synthetic-cause-logreg-v3"
MODEL_NAME = "MedFlow Educational Symptom Cause Model"
MODEL_TYPE = "Two scikit-learn Logistic Regression pipelines"
SERVER_MODE = "FastAPI model server loading a saved pre-trained joblib artifact"

SYMPTOM_FEATURES = [
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
]

FEATURE_COLUMNS = SYMPTOM_FEATURES + ["symptom_duration_days", "age_group"]


def build_pipeline() -> Pipeline:
    numeric_features = SYMPTOM_FEATURES + ["symptom_duration_days"]
    categorical_features = ["age_group"]

    preprocessor = ColumnTransformer(
        transformers=[
            ("numeric", "passthrough", numeric_features),
            ("age_group", OneHotEncoder(handle_unknown="ignore"), categorical_features),
        ]
    )

    classifier = LogisticRegression(
        max_iter=2000,
        class_weight="balanced",
        random_state=RANDOM_STATE,
    )

    return Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("classifier", classifier),
        ]
    )


def evaluate_model(name: str, pipeline: Pipeline, x_test: pd.DataFrame, y_test: pd.Series) -> dict:
    predictions = pipeline.predict(x_test)
    accuracy = accuracy_score(y_test, predictions)

    print(f"{name} accuracy: {accuracy:.4f}")
    print(f"{name} classification report:")
    print(classification_report(y_test, predictions, digits=3))
    print(f"{name} confusion matrix:")
    print(confusion_matrix(y_test, predictions))

    return {
        "accuracy": round(float(accuracy), 4),
        "classificationReport": classification_report(
            y_test,
            predictions,
            digits=3,
            output_dict=True,
        ),
    }


def main() -> None:
    if not DATA_PATH.exists():
        raise FileNotFoundError("Synthetic dataset not found. Run generate_data.py first.")

    dataset = pd.read_csv(DATA_PATH)
    required_columns = set(FEATURE_COLUMNS + ["target_pattern", "target_cause"])
    missing_columns = sorted(required_columns.difference(dataset.columns))
    if missing_columns:
        raise ValueError(f"Synthetic dataset is missing columns: {missing_columns}")

    x = dataset[FEATURE_COLUMNS]
    y_pattern = dataset["target_pattern"]
    y_cause = dataset["target_cause"]

    x_train, x_test, y_pattern_train, y_pattern_test, y_cause_train, y_cause_test = train_test_split(
        x,
        y_pattern,
        y_cause,
        test_size=0.20,
        random_state=RANDOM_STATE,
        stratify=y_cause,
    )

    pattern_pipeline = build_pipeline()
    cause_pipeline = build_pipeline()

    pattern_pipeline.fit(x_train, y_pattern_train)
    cause_pipeline.fit(x_train, y_cause_train)

    pattern_metrics = evaluate_model("Pattern model", pattern_pipeline, x_test, y_pattern_test)
    cause_metrics = evaluate_model("Cause model", cause_pipeline, x_test, y_cause_test)

    artifact = {
        "modelName": MODEL_NAME,
        "modelType": MODEL_TYPE,
        "modelVersion": MODEL_VERSION,
        "serverMode": SERVER_MODE,
        "featureColumns": FEATURE_COLUMNS,
        "supportedPatterns": sorted(y_pattern.unique().tolist()),
        "supportedCauses": sorted(y_cause.unique().tolist()),
        "trainingRows": int(len(dataset)),
        "patternAccuracy": pattern_metrics["accuracy"],
        "causeAccuracy": cause_metrics["accuracy"],
        "patternModel": pattern_pipeline,
        "causeModel": cause_pipeline,
    }

    metrics = {
        "modelName": MODEL_NAME,
        "modelType": MODEL_TYPE,
        "modelVersion": MODEL_VERSION,
        "trainingRows": int(len(dataset)),
        "patternAccuracy": pattern_metrics["accuracy"],
        "causeAccuracy": cause_metrics["accuracy"],
        "supportedPatterns": artifact["supportedPatterns"],
        "supportedCauses": artifact["supportedCauses"],
        "note": "Synthetic educational data only; not validated for medical diagnosis.",
    }

    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(artifact, MODEL_PATH)
    METRICS_PATH.write_text(json.dumps(metrics, indent=2), encoding="utf-8")

    print(f"Saved model artifact to {MODEL_PATH}")
    print(f"Saved training metrics to {METRICS_PATH}")


if __name__ == "__main__":
    main()
