from typing import Optional, Literal, Dict, Any, List, Union
from pydantic import BaseModel, Field, ConfigDict
from app.models.feature_vector import FeatureVector
from app.models.prediction_result import PredictRiskResponse

class FeatureAttribution(BaseModel):
    """
    Shapley feature attribution explaining an individual feature's contribution.
    """
    feature: str = Field(..., description="Canonical technical feature name")
    display_name: str = Field(..., description="Human-readable clinical feature title")
    value: Any = Field(..., description="Observed patient value")
    shap_value: float = Field(..., description="Signed Shapley value attribution")
    direction: Literal["increases_risk", "decreases_risk", "neutral"] = Field(
        ..., description="Direction of contribution to model predicted risk"
    )
    impact: Literal["low", "medium", "high"] = Field(
        ..., description="Relative magnitude category of attribution"
    )
    narrative: str = Field(..., description="Concise, non-diagnostic narrative explanation")


class ShapExplanation(BaseModel):
    """
    Complete SHAP explainability package for patient risk prediction.
    """
    base_value: float = Field(..., description="Expected baseline log-odds / risk across reference population")
    shap_values: Dict[str, float] = Field(..., description="Complete dictionary of 18 signed SHAP values")
    top_risk_drivers: List[FeatureAttribution] = Field(
        ..., description="Features that push the prediction toward higher risk (phi > 0)"
    )
    protective_factors: List[FeatureAttribution] = Field(
        ..., description="Features that buffer or lower predicted risk (phi < 0)"
    )
    summary_narrative: str = Field(..., description="Overall narrative summary of risk drivers")


class CounterfactualFeatureTarget(BaseModel):
    """
    Actionable target for a single modifiable feature.
    """
    feature: str = Field(..., description="Technical feature identifier")
    display_name: str = Field(..., description="Human-readable title")
    current_value: Union[float, str] = Field(..., description="Baseline patient value")
    target_value: Union[float, str] = Field(..., description="Actionable simulated target value")
    unit: Optional[str] = Field(None, description="Physical unit of measurement")
    change_delta: Optional[float] = Field(None, description="Difference (target_value - current_value) if continuous")
    allowed_range: List[Any] = Field(..., description="Clinically safe range bounds [min, max]")
    direction: str = Field(..., description="Required direction of change ('decrease' or 'increase')")
    modifiable: bool = Field(True, description="Strict confirmation of modifiability")


class CounterfactualPlan(BaseModel):
    """
    Actionable lifestyle/biomarker target scenario simulated through the model.
    """
    plan_id: str = Field(..., description="Unique plan identifier")
    title: str = Field(..., description="Descriptive title of target scenario")
    simulated_risk_score: float = Field(..., ge=0.0, le=1.0, description="Predicted risk score after modification")
    simulated_risk_category: Literal["LOW", "MODERATE", "ELEVATED", "HIGH"] = Field(
        ..., description="Target risk category"
    )
    risk_reduction: float = Field(..., description="Absolute risk reduction (original - simulated)")
    tier_transition_achieved: bool = Field(
        ..., description="True if modification moves the patient into a lower risk category"
    )
    changed_features: List[CounterfactualFeatureTarget] = Field(
        ..., description="List of modifiable features adjusted in this plan"
    )
    rationale: str = Field(..., description="Model-oriented explanation of the simulated benefit")


class CounterfactualResult(BaseModel):
    """
    Counterfactual recommendation response container.
    """
    counterfactual_available: bool = Field(..., description="Whether valid counterfactuals were found")
    status: Literal["SUCCESS", "NO_VALID_COUNTERFACTUAL_FOUND"] = Field(
        ..., description="Search status"
    )
    message: str = Field(..., description="Descriptive status message or explanation")
    plans: List[CounterfactualPlan] = Field(default_factory=list, description="Ranked counterfactual target plans")


class ExplainRiskRequest(BaseModel):
    """
    Explainability request accepting the same feature representation as prediction.
    """
    model_config = ConfigDict(extra="forbid")

    patient_uuid: Optional[str] = Field(None, description="Pseudonymized patient UUID")
    evaluation_mode: Optional[Literal["INTAKE", "PRELIMINARY_INTAKE", "LONGITUDINAL", "LONGITUDINAL_ROBUST"]] = Field(
        "LONGITUDINAL", description="Evaluation context mode"
    )
    features: FeatureVector
    top_k: Optional[int] = Field(
        5, ge=1, le=18, description="Number of top SHAP risk drivers and protective factors to return"
    )
    generate_counterfactuals: Optional[bool] = Field(
        True, description="Whether to compute constrained counterfactual targets"
    )


class ExplainRiskResponse(BaseModel):
    """
    Unified Explainable AI response combining prediction, SHAP attribution, and counterfactuals.
    """
    prediction: PredictRiskResponse = Field(..., description="Core calibrated risk prediction")
    explanation: ShapExplanation = Field(..., description="SHAP feature attribution decomposition")
    counterfactuals: CounterfactualResult = Field(..., description="Actionable constrained counterfactual targets")
    regulatory_disclaimer: str = Field(..., description="Clinical non-diagnostic decision-support disclaimer")
