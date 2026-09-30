from typing import Optional, Literal, Dict, Any, List
from pydantic import BaseModel, Field

class PredictRiskResponse(BaseModel):
    """
    Standard risk prediction response matching API_DESIGN.md section 3.2 and FR-7.2.
    Output is strictly described as a decision-support risk signal, not a clinical diagnosis.
    """
    model_version: str = Field(..., description="Trained model identifier and version")
    overall_risk_score: float = Field(..., ge=0.0, le=1.0, description="Continuous calibrated risk signal in [0.0, 1.0]")
    risk_category: Literal["LOW", "MODERATE", "ELEVATED", "HIGH"] = Field(
        ..., description="Standardized cardiometabolic risk category"
    )
    prediction: int = Field(..., description="Binary threshold prediction (1 if risk >= 0.50 else 0)")
    probability: float = Field(..., ge=0.0, le=1.0, description="Calibrated posterior probability of adverse decompensation")
    calibrated: bool = Field(True, description="Indicates if output probabilities are calibrated via Platt/sigmoid scaling")
    confidence_level: Optional[str] = Field(None, description="Confidence classification (PRELIMINARY_INTAKE or LONGITUDINAL_ROBUST)")
    regulatory_disclaimer: str = Field(..., description="Mandatory non-diagnostic clinical disclaimer")


class HealthResponse(BaseModel):
    """
    Service health and non-sensitive metadata for /ml/v1/health.
    """
    status: Literal["HEALTHY", "DEGRADED", "UNHEALTHY"] = Field(..., description="Service operating status")
    service_name: str = Field(..., description="Microservice name")
    service_version: str = Field(..., description="Microservice release version")
    modelVersion: str = Field(..., description="Loaded ML model version")
    model_loaded: bool = Field(..., description="Flag indicating if inference pipeline is active and loaded")
    calibrated: bool = Field(..., description="Whether model outputs are calibrated")
    shapExplainerType: Optional[str] = Field("TreeExplainer", description="Active SHAP explainer algorithm")
    featuresSupported: int = Field(18, description="Number of expected input dimensions")


class ModelMetadata(BaseModel):
    """
    Metadata associated with a trained and serialized model artifact.
    """
    model_name: str
    model_version: str
    algorithm: str
    training_timestamp: str
    dataset_version: str
    dataset_samples: int
    train_samples: int
    test_samples: int
    feature_columns: List[str]
    evaluation_metrics: Dict[str, float]
    calibrated: bool
    brier_score: float
    roc_auc: float
    pr_auc: float
