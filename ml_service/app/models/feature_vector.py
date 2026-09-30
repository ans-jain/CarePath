from typing import Optional, Literal
from pydantic import BaseModel, Field, ConfigDict
import uuid

# Deterministic feature ordering - Single source of truth across dataset, model, and inference
FEATURE_COLUMNS = [
    "age",
    "biological_sex",
    "bmi",
    "smoking_status",
    "systolic_bp_current",
    "diastolic_bp_current",
    "heart_rate_current",
    "fasting_glucose_current",
    "hba1c_current",
    "cholesterol_total",
    "cholesterol_hdl",
    "cholesterol_ldl",
    "triglycerides",
    "sleep_hours_current",
    "systolic_bp_ewma_delta",
    "fasting_glucose_ewma_delta",
    "systolic_bp_slope_14d",
    "fasting_glucose_slope_14d"
]

class FeatureVector(BaseModel):
    """
    18-dimensional feature vector defined by ML_DESIGN.md Section 2.
    """
    model_config = ConfigDict(extra="forbid")

    age: float = Field(..., ge=18.0, le=120.0, description="Patient age in years [18, 120]")
    biological_sex: Literal["FEMALE", "MALE", "0", "1", 0, 1] = Field(
        ..., description="Biological sex (FEMALE/0 or MALE/1)"
    )
    bmi: float = Field(..., ge=10.0, le=100.0, description="Body Mass Index [10.0, 100.0]")
    smoking_status: Literal["NEVER", "FORMER", "CURRENT", "0", "1", "2", 0, 1, 2] = Field(
        ..., description="Smoking status (NEVER, FORMER, CURRENT)"
    )
    systolic_bp_current: float = Field(..., ge=40.0, le=300.0, description="Current Systolic BP (mmHg)")
    diastolic_bp_current: float = Field(..., ge=30.0, le=200.0, description="Current Diastolic BP (mmHg)")
    heart_rate_current: float = Field(..., ge=20.0, le=260.0, description="Current Resting Heart Rate (bpm)")
    fasting_glucose_current: float = Field(..., ge=20.0, le=800.0, description="Fasting Blood Glucose (mg/dL)")
    
    # Optional lab biomarkers (may be pending during initial intake)
    hba1c_current: Optional[float] = Field(None, ge=3.0, le=20.0, description="Glycated Hemoglobin (%)")
    cholesterol_total: Optional[float] = Field(None, ge=50.0, le=800.0, description="Total Cholesterol (mg/dL)")
    cholesterol_hdl: Optional[float] = Field(None, ge=5.0, le=200.0, description="HDL Cholesterol (mg/dL)")
    cholesterol_ldl: Optional[float] = Field(None, ge=10.0, le=600.0, description="LDL Cholesterol (mg/dL)")
    triglycerides: Optional[float] = Field(None, ge=10.0, le=2000.0, description="Triglycerides (mg/dL)")
    sleep_hours_current: Optional[float] = Field(7.0, gt=0.0, le=24.0, description="Average sleep duration (hrs/night)")
    
    # Longitudinal features (default to 0.0 for cold-start intake)
    systolic_bp_ewma_delta: Optional[float] = Field(0.0, description="Current vs 30-day baseline EWMA delta (mmHg)")
    fasting_glucose_ewma_delta: Optional[float] = Field(0.0, description="Current vs 30-day baseline EWMA delta (mg/dL)")
    systolic_bp_slope_14d: Optional[float] = Field(0.0, description="14-day OLS trajectory slope (mmHg/day)")
    fasting_glucose_slope_14d: Optional[float] = Field(0.0, description="14-day OLS trajectory slope (mg/dL/day)")


class PredictRiskRequest(BaseModel):
    """
    Inference request matching API_DESIGN.md section 3.2.
    """
    model_config = ConfigDict(extra="forbid")

    patient_uuid: Optional[str] = Field(None, description="Pseudonymized patient UUID")
    evaluation_mode: Optional[Literal["INTAKE", "PRELIMINARY_INTAKE", "LONGITUDINAL", "LONGITUDINAL_ROBUST"]] = Field(
        "LONGITUDINAL", description="Evaluation context mode"
    )
    features: FeatureVector
