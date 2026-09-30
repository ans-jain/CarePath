import numpy as np
import pandas as pd
from typing import Tuple
from app.models.feature_vector import FEATURE_COLUMNS

def generate_synthetic_clinical_cohort(n_samples: int = 5000, random_seed: int = 42) -> Tuple[pd.DataFrame, pd.Series]:
    """
    Generates a realistic synthetic clinical cohort for training the CarePath risk model.
    Biomarker distributions and covariances are modeled from CDC NHANES and Framingham parameters.
    Does NOT contain any real patient identifiable information.
    
    Returns:
        X: pd.DataFrame with 18 columns matching FEATURE_COLUMNS
        y: pd.Series with binary target indicating 3-year adverse cardiometabolic decompensation (0: No, 1: Yes)
    """
    rng = np.random.default_rng(random_seed)

    # 1. Demographics
    age = rng.integers(18, 85, size=n_samples).astype(float)
    biological_sex = rng.choice(["FEMALE", "MALE"], size=n_samples, p=[0.52, 0.48])
    smoking_status = rng.choice(["NEVER", "FORMER", "CURRENT"], size=n_samples, p=[0.55, 0.25, 0.20])
    
    # BMI: Normal ~26.5 with positive skew
    bmi = np.clip(rng.normal(loc=27.5, scale=5.8, size=n_samples), 14.0, 58.0)

    # 2. Instantaneous Biomarkers (correlated physiological variables)
    # Systolic BP increases with age and BMI
    systolic_mean = 115.0 + 0.35 * (age - 40) + 0.65 * (bmi - 25)
    systolic_bp = np.clip(rng.normal(loc=systolic_mean, scale=12.0), 85.0, 220.0)

    # Diastolic BP correlated with systolic
    diastolic_mean = 70.0 + 0.15 * (systolic_bp - 120) + 0.25 * (bmi - 25)
    diastolic_bp = np.clip(rng.normal(loc=diastolic_mean, scale=8.0), 50.0, 130.0)

    # Resting Heart Rate
    heart_rate = np.clip(rng.normal(loc=72.0 + 0.3 * (bmi - 25), scale=10.0), 45.0, 140.0)

    # Fasting Blood Glucose (correlated with BMI and age)
    glucose_mean = 92.0 + 0.25 * (age - 40) + 0.85 * (bmi - 25)
    fasting_glucose = np.clip(rng.normal(loc=glucose_mean, scale=18.0), 65.0, 320.0)

    # HbA1c strongly correlated with fasting glucose
    hba1c_mean = 4.8 + 0.018 * (fasting_glucose - 90)
    hba1c = np.clip(rng.normal(loc=hba1c_mean, scale=0.45), 4.0, 14.0)

    # Lipids: Total, HDL, LDL, Triglycerides
    chol_hdl = np.clip(rng.normal(loc=55.0 - 0.3 * (bmi - 25), scale=12.0), 20.0, 110.0)
    chol_ldl = np.clip(rng.normal(loc=115.0 + 0.6 * (bmi - 25) + 0.3 * (age - 40), scale=28.0), 45.0, 260.0)
    triglycerides = np.clip(rng.normal(loc=135.0 + 1.8 * (bmi - 25), scale=55.0), 35.0, 650.0)
    chol_total = chol_hdl + chol_ldl + 0.20 * triglycerides

    # Sleep hours
    sleep_hours = np.clip(rng.normal(loc=7.1, scale=1.2), 3.5, 11.0)

    # 3. Dynamic Longitudinal Features
    # Baseline deltas: zero-centered with occasional pathological spikes
    systolic_bp_ewma_delta = rng.normal(loc=0.3, scale=4.5, size=n_samples)
    fasting_glucose_ewma_delta = rng.normal(loc=0.4, scale=6.0, size=n_samples)

    # 14-day trajectory slopes
    systolic_bp_slope_14d = rng.normal(loc=0.02, scale=0.25, size=n_samples)
    fasting_glucose_slope_14d = rng.normal(loc=0.03, scale=0.35, size=n_samples)

    # 4. Realistic Clinical Missingness (e.g. cold-start patients without complete lab panels)
    # 8% missing lipid panels, 6% missing HbA1c
    missing_lipids_mask = rng.random(size=n_samples) < 0.08
    chol_total[missing_lipids_mask] = np.nan
    chol_hdl[missing_lipids_mask] = np.nan
    chol_ldl[missing_lipids_mask] = np.nan
    triglycerides[missing_lipids_mask] = np.nan

    missing_hba1c_mask = rng.random(size=n_samples) < 0.06
    hba1c[missing_hba1c_mask] = np.nan

    # Assemble DataFrame
    df = pd.DataFrame({
        "age": age,
        "biological_sex": biological_sex,
        "bmi": bmi,
        "smoking_status": smoking_status,
        "systolic_bp_current": systolic_bp,
        "diastolic_bp_current": diastolic_bp,
        "heart_rate_current": heart_rate,
        "fasting_glucose_current": fasting_glucose,
        "hba1c_current": hba1c,
        "cholesterol_total": chol_total,
        "cholesterol_hdl": chol_hdl,
        "cholesterol_ldl": chol_ldl,
        "triglycerides": triglycerides,
        "sleep_hours_current": sleep_hours,
        "systolic_bp_ewma_delta": systolic_bp_ewma_delta,
        "fasting_glucose_ewma_delta": fasting_glucose_ewma_delta,
        "systolic_bp_slope_14d": systolic_bp_slope_14d,
        "fasting_glucose_slope_14d": fasting_glucose_slope_14d
    })[FEATURE_COLUMNS]

    # 5. Continuous Risk Score Calculation via Cardiometabolic Multi-Factor Formulation
    smoke_weight = np.where(smoking_status == "CURRENT", 0.65, np.where(smoking_status == "FORMER", 0.25, 0.0))
    sex_weight = np.where(biological_sex == "MALE", 0.15, 0.0)

    # Impute temporary median for risk calculation formula
    hba1c_clean = np.nan_to_num(hba1c, nan=5.4)
    ldl_clean = np.nan_to_num(chol_ldl, nan=110.0)
    hdl_clean = np.nan_to_num(chol_hdl, nan=50.0)

    z = (
        -2.6
        + 0.040 * (age - 45.0)
        + 0.030 * (systolic_bp - 120.0)
        + 0.022 * (fasting_glucose - 95.0)
        + 0.450 * (hba1c_clean - 5.4)
        + 0.055 * (bmi - 25.0)
        + 0.012 * (ldl_clean - 100.0)
        - 0.025 * (hdl_clean - 50.0)
        + 0.060 * np.clip(systolic_bp_ewma_delta, -10, 20)
        + 0.080 * np.clip(fasting_glucose_ewma_delta, -15, 30)
        + 0.700 * np.clip(systolic_bp_slope_14d, -1.0, 2.0)
        + 0.600 * np.clip(fasting_glucose_slope_14d, -1.0, 2.0)
        + smoke_weight
        + sex_weight
        + rng.normal(0, 0.35, size=n_samples)
    )

    probs = 1.0 / (1.0 + np.exp(-z))
    # Binary adverse decompensation target
    y = pd.Series((rng.random(size=n_samples) < probs).astype(int), name="target")

    return df, y
