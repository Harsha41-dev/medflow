from enum import Enum

from pydantic import BaseModel, ConfigDict, Field


class AgeGroup(str, Enum):
    CHILD = "CHILD"
    ADULT = "ADULT"
    OLDER_ADULT = "OLDER_ADULT"


class SymptomPredictionRequest(BaseModel):
    fever: int = Field(..., ge=0, le=1)
    cough: int = Field(..., ge=0, le=1)
    soreThroat: int = Field(..., ge=0, le=1)
    runnyNose: int = Field(..., ge=0, le=1)
    sneezing: int = Field(..., ge=0, le=1)
    headache: int = Field(..., ge=0, le=1)
    fatigue: int = Field(..., ge=0, le=1)
    nausea: int = Field(..., ge=0, le=1)
    vomiting: int = Field(..., ge=0, le=1)
    abdominalPain: int = Field(..., ge=0, le=1)
    diarrhea: int = Field(..., ge=0, le=1)
    chestDiscomfort: int = Field(..., ge=0, le=1)
    shortnessOfBreath: int = Field(..., ge=0, le=1)
    bodyAche: int = Field(..., ge=0, le=1)
    jointPain: int = Field(..., ge=0, le=1)
    dizziness: int = Field(..., ge=0, le=1)
    lightSensitivity: int = Field(..., ge=0, le=1)
    symptomDurationDays: int = Field(..., ge=0)
    ageGroup: AgeGroup

    model_config = ConfigDict(extra="forbid")


class PatternConfidence(BaseModel):
    pattern: str
    confidence: float


class CauseConfidence(BaseModel):
    cause: str
    confidence: float


class SymptomFactor(BaseModel):
    field: str
    label: str
    value: str
    contribution: float


class SafetyFlag(BaseModel):
    code: str
    severity: str
    message: str


class PossibleCause(BaseModel):
    title: str
    reason: str


class LikelyCause(BaseModel):
    code: str
    title: str
    confidence: float
    confidenceLevel: str
    evidence: list[str]
    uncertaintyNotes: list[str]
    nextSteps: list[str]


class SymptomPredictionResponse(BaseModel):
    predictedPattern: str
    confidence: float
    alternatives: list[PatternConfidence]
    likelyCause: LikelyCause
    causeAlternatives: list[CauseConfidence]
    modelVersion: str
    confidenceLevel: str
    contributingFactors: list[SymptomFactor]
    safetyFlags: list[SafetyFlag]
    possibleCauses: list[PossibleCause]
    suggestedDoctorQuestions: list[str]
    reviewPriority: str
    reviewMessage: str
    disclaimer: str


class SymptomModelInfoResponse(BaseModel):
    modelName: str
    modelType: str
    modelVersion: str
    serverMode: str
    featureCount: int
    patternCount: int
    supportedPatterns: list[str]
    trainingData: str
    explanationMethod: str
    disclaimer: str
