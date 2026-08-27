import csv
import random
from pathlib import Path

RANDOM_STATE = 42
ROWS_PER_CAUSE = 500
NOISE_RATE = 0.015

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

AGE_GROUPS = ["CHILD", "ADULT", "OLDER_ADULT"]

CAUSE_PROFILES = {
    "COMMON_COLD_LIKE": {
        "pattern": "RESPIRATORY_PATTERN",
        "duration": (2, 7),
        "age_weights": [0.26, 0.58, 0.16],
        "symptoms": {
            "fever": 0.16,
            "cough": 0.88,
            "sore_throat": 0.76,
            "runny_nose": 0.78,
            "sneezing": 0.42,
            "headache": 0.20,
            "fatigue": 0.30,
            "body_ache": 0.10,
        },
    },
    "BRONCHITIS_AIRWAY_IRRITATION_LIKE": {
        "pattern": "RESPIRATORY_PATTERN",
        "duration": (5, 18),
        "age_weights": [0.14, 0.58, 0.28],
        "symptoms": {
            "fever": 0.18,
            "cough": 0.96,
            "sore_throat": 0.14,
            "fatigue": 0.45,
            "chest_discomfort": 0.66,
            "shortness_of_breath": 0.54,
            "body_ache": 0.10,
        },
    },
    "FLU_LIKE_VIRAL_ILLNESS": {
        "pattern": "VIRAL_LIKE_PATTERN",
        "duration": (1, 7),
        "age_weights": [0.20, 0.62, 0.18],
        "symptoms": {
            "fever": 0.92,
            "cough": 0.38,
            "sore_throat": 0.28,
            "runny_nose": 0.16,
            "headache": 0.72,
            "fatigue": 0.90,
            "body_ache": 0.90,
            "joint_pain": 0.42,
        },
    },
    "ALLERGIC_RHINITIS_LIKE": {
        "pattern": "ALLERGY_LIKE_PATTERN",
        "duration": (3, 30),
        "age_weights": [0.22, 0.62, 0.16],
        "symptoms": {
            "fever": 0.01,
            "cough": 0.16,
            "sore_throat": 0.12,
            "runny_nose": 0.96,
            "sneezing": 0.96,
            "headache": 0.18,
            "fatigue": 0.14,
            "shortness_of_breath": 0.04,
        },
    },
    "ACUTE_GASTROENTERITIS_LIKE": {
        "pattern": "GASTROINTESTINAL_PATTERN",
        "duration": (1, 6),
        "age_weights": [0.24, 0.58, 0.18],
        "symptoms": {
            "fever": 0.35,
            "fatigue": 0.46,
            "nausea": 0.88,
            "vomiting": 0.78,
            "abdominal_pain": 0.78,
            "diarrhea": 0.90,
            "dizziness": 0.36,
        },
    },
    "FOOD_RELATED_STOMACH_UPSET_LIKE": {
        "pattern": "GASTROINTESTINAL_PATTERN",
        "duration": (0, 3),
        "age_weights": [0.18, 0.66, 0.16],
        "symptoms": {
            "fever": 0.06,
            "fatigue": 0.32,
            "nausea": 0.84,
            "vomiting": 0.38,
            "abdominal_pain": 0.80,
            "diarrhea": 0.28,
            "dizziness": 0.12,
        },
    },
    "MIGRAINE_LIKE_HEADACHE": {
        "pattern": "MIGRAINE_LIKE_PATTERN",
        "duration": (0, 5),
        "age_weights": [0.10, 0.72, 0.18],
        "symptoms": {
            "fever": 0.02,
            "headache": 0.96,
            "fatigue": 0.32,
            "nausea": 0.62,
            "vomiting": 0.26,
            "dizziness": 0.42,
            "light_sensitivity": 0.92,
        },
    },
    "MUSCLE_STRAIN_LIKE": {
        "pattern": "MUSCULOSKELETAL_PATTERN",
        "duration": (1, 10),
        "age_weights": [0.12, 0.68, 0.20],
        "symptoms": {
            "fever": 0.02,
            "fatigue": 0.22,
            "body_ache": 0.92,
            "joint_pain": 0.18,
            "headache": 0.08,
        },
    },
    "JOINT_INFLAMMATION_LIKE": {
        "pattern": "MUSCULOSKELETAL_PATTERN",
        "duration": (10, 30),
        "age_weights": [0.10, 0.52, 0.38],
        "symptoms": {
            "fever": 0.12,
            "fatigue": 0.36,
            "body_ache": 0.38,
            "joint_pain": 0.94,
            "dizziness": 0.06,
        },
    },
    "NON_SPECIFIC_SYMPTOM_CLUSTER": {
        "pattern": "GENERAL_UNSPECIFIED_PATTERN",
        "duration": (0, 4),
        "age_weights": [0.18, 0.62, 0.20],
        "symptoms": {
            "fever": 0.08,
            "cough": 0.06,
            "sore_throat": 0.06,
            "runny_nose": 0.06,
            "sneezing": 0.05,
            "headache": 0.14,
            "fatigue": 0.22,
            "nausea": 0.08,
            "vomiting": 0.04,
            "abdominal_pain": 0.06,
            "diarrhea": 0.05,
            "body_ache": 0.10,
            "joint_pain": 0.06,
            "dizziness": 0.12,
            "light_sensitivity": 0.05,
        },
    },
}


def generate_row(cause: str, rng: random.Random) -> dict[str, object]:
    profile = CAUSE_PROFILES[cause]
    rules = profile["symptoms"]
    row: dict[str, object] = {}

    for feature in SYMPTOM_FEATURES:
        probability = rules.get(feature, 0.04)
        value = 1 if rng.random() < probability else 0

        if rng.random() < NOISE_RATE:
            value = 1 - value

        row[feature] = value

    min_days, max_days = profile["duration"]
    row["symptom_duration_days"] = rng.randint(min_days, max_days)
    row["age_group"] = rng.choices(AGE_GROUPS, weights=profile["age_weights"], k=1)[0]
    row["target_pattern"] = profile["pattern"]
    row["target_cause"] = cause
    return row


def main() -> None:
    rng = random.Random(RANDOM_STATE)
    output_path = Path(__file__).parent / "data" / "synthetic_symptoms.csv"
    output_path.parent.mkdir(parents=True, exist_ok=True)

    fieldnames = SYMPTOM_FEATURES + [
        "symptom_duration_days",
        "age_group",
        "target_pattern",
        "target_cause",
    ]
    rows = []

    for cause in CAUSE_PROFILES:
        for _ in range(ROWS_PER_CAUSE):
            rows.append(generate_row(cause, rng))

    rng.shuffle(rows)

    with output_path.open("w", newline="", encoding="utf-8") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Generated {len(rows)} synthetic rows at {output_path}")
    print(f"Cause labels: {len(CAUSE_PROFILES)}")


if __name__ == "__main__":
    main()
