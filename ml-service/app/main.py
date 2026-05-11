from fastapi import FastAPI

from app.predictor import SymptomPatternPredictor
from app.schemas import SymptomModelInfoResponse, SymptomPredictionRequest, SymptomPredictionResponse

app = FastAPI(
    title="MedFlow Symptom Pattern Classifier",
    description="Educational ML-assisted broad symptom-pattern classification API.",
    version="1.0.0",
)

predictor = SymptomPatternPredictor()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.get("/model-info", response_model=SymptomModelInfoResponse)
def model_info() -> dict:
    return predictor.model_info()


@app.post("/predict", response_model=SymptomPredictionResponse)
def predict(request: SymptomPredictionRequest) -> dict:
    return predictor.predict(request)
