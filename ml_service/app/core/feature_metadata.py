from typing import Dict, Any, Optional

FEATURE_METADATA: Dict[str, Dict[str, Any]] = {
    "age": {
        "display_name": "Patient Age",
        "unit": "years",
        "category": "Static Demographics",
        "is_modifiable": False,
        "description": "Chronological patient age in years."
    },
    "biological_sex": {
        "display_name": "Biological Sex",
        "unit": None,
        "category": "Static Demographics",
        "is_modifiable": False,
        "description": "Biological sex at birth (FEMALE or MALE)."
    },
    "bmi": {
        "display_name": "Body Mass Index (BMI)",
        "unit": "kg/m²",
        "category": "Static Demographics",
        "is_modifiable": True,
        "min_bound": 18.5,
        "max_bound": 50.0,
        "step_size": 0.5,
        "max_delta": 3.0,
        "optimal_target": 23.5,
        "preferred_direction": "decrease",
        "description": "Body mass index reflecting weight-to-height ratio."
    },
    "smoking_status": {
        "display_name": "Smoking Status",
        "unit": None,
        "category": "Static Demographics",
        "is_modifiable": True,
        "min_bound": 0.0,
        "max_bound": 2.0,
        "step_size": 1.0,
        "optimal_target": 0.0,
        "preferred_direction": "decrease",
        "description": "Tobacco use status (NEVER, FORMER, CURRENT)."
    },
    "systolic_bp_current": {
        "display_name": "Current Systolic Blood Pressure",
        "unit": "mmHg",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 100.0,
        "max_bound": 200.0,
        "step_size": 5.0,
        "max_delta": 35.0,
        "optimal_target": 118.0,
        "preferred_direction": "decrease",
        "description": "Current resting systolic blood pressure."
    },
    "diastolic_bp_current": {
        "display_name": "Current Diastolic Blood Pressure",
        "unit": "mmHg",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 65.0,
        "max_bound": 120.0,
        "step_size": 5.0,
        "max_delta": 25.0,
        "optimal_target": 76.0,
        "preferred_direction": "decrease",
        "description": "Current resting diastolic blood pressure."
    },
    "heart_rate_current": {
        "display_name": "Current Resting Heart Rate",
        "unit": "bpm",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 50.0,
        "max_bound": 140.0,
        "step_size": 4.0,
        "max_delta": 20.0,
        "optimal_target": 68.0,
        "preferred_direction": "decrease",
        "description": "Resting pulse rate."
    },
    "fasting_glucose_current": {
        "display_name": "Current Fasting Blood Glucose",
        "unit": "mg/dL",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 75.0,
        "max_bound": 300.0,
        "step_size": 5.0,
        "max_delta": 55.0,
        "optimal_target": 92.0,
        "preferred_direction": "decrease",
        "description": "Fasting blood sugar level."
    },
    "hba1c_current": {
        "display_name": "Glycated Hemoglobin (HbA1c)",
        "unit": "%",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 4.8,
        "max_bound": 12.0,
        "step_size": 0.2,
        "max_delta": 1.0,
        "optimal_target": 5.3,
        "preferred_direction": "decrease",
        "description": "3-month average blood glucose percentage."
    },
    "cholesterol_total": {
        "display_name": "Total Serum Cholesterol",
        "unit": "mg/dL",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 140.0,
        "max_bound": 350.0,
        "step_size": 10.0,
        "max_delta": 40.0,
        "optimal_target": 180.0,
        "preferred_direction": "decrease",
        "description": "Total serum lipid content."
    },
    "cholesterol_hdl": {
        "display_name": "HDL ('Good') Cholesterol",
        "unit": "mg/dL",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 30.0,
        "max_bound": 90.0,
        "step_size": 5.0,
        "max_delta": 15.0,
        "optimal_target": 65.0,
        "preferred_direction": "increase",
        "description": "High-density lipoprotein cholesterol buffering vascular risk."
    },
    "cholesterol_ldl": {
        "display_name": "LDL ('Bad') Cholesterol",
        "unit": "mg/dL",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 60.0,
        "max_bound": 250.0,
        "step_size": 10.0,
        "max_delta": 35.0,
        "optimal_target": 95.0,
        "preferred_direction": "decrease",
        "description": "Low-density lipoprotein cholesterol."
    },
    "triglycerides": {
        "display_name": "Serum Triglycerides",
        "unit": "mg/dL",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 60.0,
        "max_bound": 500.0,
        "step_size": 15.0,
        "max_delta": 50.0,
        "optimal_target": 115.0,
        "preferred_direction": "decrease",
        "description": "Circulating blood fat lipid concentration."
    },
    "sleep_hours_current": {
        "display_name": "Average Sleep Duration",
        "unit": "hrs/night",
        "category": "Instantaneous Biomarkers",
        "is_modifiable": True,
        "min_bound": 5.0,
        "max_bound": 9.5,
        "step_size": 0.5,
        "max_delta": 2.0,
        "optimal_target": 7.5,
        "preferred_direction": "increase",
        "description": "Average nightly sleep duration."
    },
    "systolic_bp_ewma_delta": {
        "display_name": "Systolic BP 30-Day Baseline Deviation",
        "unit": "mmHg",
        "category": "Longitudinal Dynamics",
        "is_modifiable": False,
        "description": "Difference between current systolic BP and patient's 30-day EWMA baseline."
    },
    "fasting_glucose_ewma_delta": {
        "display_name": "Fasting Glucose 30-Day Baseline Deviation",
        "unit": "mg/dL",
        "category": "Longitudinal Dynamics",
        "is_modifiable": False,
        "description": "Difference between current glucose and patient's 30-day EWMA baseline."
    },
    "systolic_bp_slope_14d": {
        "display_name": "14-Day Systolic BP Trajectory Slope",
        "unit": "mmHg/day",
        "category": "Longitudinal Dynamics",
        "is_modifiable": False,
        "description": "14-day Ordinary Least Squares linear trajectory slope of systolic BP."
    },
    "fasting_glucose_slope_14d": {
        "display_name": "14-Day Fasting Glucose Trajectory Slope",
        "unit": "mg/dL/day",
        "category": "Longitudinal Dynamics",
        "is_modifiable": False,
        "description": "14-day Ordinary Least Squares linear trajectory slope of fasting glucose."
    }
}

def get_feature_display_name(feature_name: str) -> str:
    """Returns human-readable display label for a feature name."""
    meta = FEATURE_METADATA.get(feature_name)
    if meta:
        return meta["display_name"]
    # Fallback to Title Cased representation
    return feature_name.replace("_", " ").title()

def get_feature_unit(feature_name: str) -> Optional[str]:
    """Returns unit of measurement for a feature name if applicable."""
    meta = FEATURE_METADATA.get(feature_name)
    return meta.get("unit") if meta else None

def is_feature_modifiable(feature_name: str) -> bool:
    """Returns True if the feature can be safely modified in counterfactuals."""
    meta = FEATURE_METADATA.get(feature_name)
    return bool(meta.get("is_modifiable", False)) if meta else False

def format_feature_narrative(
    feature_name: str,
    raw_value: Any,
    shap_value: float,
    direction: str
) -> str:
    """
    Generates a concise, non-diagnostic narrative explaining how the feature
    influences the model's predicted cardiometabolic risk signal.
    """
    display_name = get_feature_display_name(feature_name)
    unit = get_feature_unit(feature_name)
    unit_str = f" {unit}" if unit else ""
    val_str = f"{raw_value}{unit_str}" if raw_value is not None else "measured value"

    # Human-readable categorical representations
    if feature_name == "biological_sex":
        val_str = "Male" if str(raw_value).upper() in ["1", "MALE", "1.0"] else "Female"
    elif feature_name == "smoking_status":
        s_map = {"0": "Non-smoker", "1": "Former smoker", "2": "Current smoker"}
        val_str = s_map.get(str(raw_value), "Non-smoker")

    if direction == "increases_risk":
        return (
            f"{display_name} ({val_str}) is a primary risk driver, "
            f"increasing the model's predicted risk score (+{abs(shap_value):.4f})."
        )
    elif direction == "decreases_risk":
        return (
            f"{display_name} ({val_str}) serves as a protective factor, "
            f"reducing the model's predicted risk score (-{abs(shap_value):.4f})."
        )
    else:
        return f"{display_name} ({val_str}) has a neutral effect on the model's predicted risk score."
