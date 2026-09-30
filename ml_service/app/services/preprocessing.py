from typing import Dict, Any, Union, List
import pandas as pd
import numpy as np
from sklearn.base import BaseEstimator, TransformerMixin
from app.models.feature_vector import FEATURE_COLUMNS, FeatureVector

class CarePathPreprocessor(BaseEstimator, TransformerMixin):
    """
    Deterministic clinical feature preprocessor for the CarePath ML pipeline.
    Reused identically between offline training and online FastAPI inference.
    """

    def __init__(self):
        self.feature_columns: List[str] = list(FEATURE_COLUMNS)
        self.median_values_: Dict[str, float] = {}

    def _encode_features(self, df: pd.DataFrame) -> pd.DataFrame:
        df = df.copy()

        # Biological Sex mapping: FEMALE -> 0.0, MALE -> 1.0
        if "biological_sex" in df.columns:
            df["biological_sex"] = df["biological_sex"].apply(
                lambda v: 0.0 if str(v).strip().upper() in ["FEMALE", "0"] else (
                    1.0 if str(v).strip().upper() in ["MALE", "1"] else 0.0
                )
            )

        # Smoking Status mapping: NEVER -> 0.0, FORMER -> 1.0, CURRENT -> 2.0
        if "smoking_status" in df.columns:
            def map_smoking(v):
                v_str = str(v).strip().upper()
                if v_str in ["NEVER", "0"]:
                    return 0.0
                elif v_str in ["FORMER", "1"]:
                    return 1.0
                elif v_str in ["CURRENT", "2"]:
                    return 2.0
                return 0.0

            df["smoking_status"] = df["smoking_status"].apply(map_smoking)

        # Ensure all columns exist and convert to numeric
        for col in self.feature_columns:
            if col not in df.columns:
                df[col] = np.nan
            else:
                df[col] = pd.to_numeric(df[col], errors="coerce")

        return df[self.feature_columns]

    def fit(self, X: Union[pd.DataFrame, np.ndarray, List[Dict[str, Any]]], y=None):
        """
        Fit preprocessor strictly on training data to prevent data leakage.
        Learns training median distributions for numerical features.
        """
        if isinstance(X, pd.DataFrame):
            df = X.copy()
        elif isinstance(X, list) and len(X) > 0 and isinstance(X[0], dict):
            df = pd.DataFrame(X)
        else:
            df = pd.DataFrame(X, columns=self.feature_columns)

        df_encoded = self._encode_features(df)

        for col in self.feature_columns:
            series = df_encoded[col]
            median_val = float(series.median(skipna=True))
            if np.isnan(median_val):
                # Fallback clinical reference medians
                fallback = {
                    "age": 45.0, "bmi": 26.0, "systolic_bp_current": 120.0,
                    "diastolic_bp_current": 80.0, "heart_rate_current": 72.0,
                    "fasting_glucose_current": 95.0, "hba1c_current": 5.4,
                    "cholesterol_total": 190.0, "cholesterol_hdl": 50.0,
                    "cholesterol_ldl": 110.0, "triglycerides": 130.0,
                    "sleep_hours_current": 7.0, "systolic_bp_ewma_delta": 0.0,
                    "fasting_glucose_ewma_delta": 0.0, "systolic_bp_slope_14d": 0.0,
                    "fasting_glucose_slope_14d": 0.0, "biological_sex": 0.0,
                    "smoking_status": 0.0
                }
                median_val = fallback.get(col, 0.0)
            self.median_values_[col] = median_val

        return self

    def transform(self, X: Union[pd.DataFrame, np.ndarray, List[Dict[str, Any]], Dict[str, Any], FeatureVector]) -> np.ndarray:
        """
        Transforms input into an 18-dimensional numpy float64 array adhering strictly to FEATURE_COLUMNS order.
        """
        if isinstance(X, FeatureVector):
            data_dict = X.model_dump()
            df = pd.DataFrame([data_dict])
        elif isinstance(X, dict):
            df = pd.DataFrame([X])
        elif isinstance(X, list) and len(X) > 0 and isinstance(X[0], dict):
            df = pd.DataFrame(X)
        elif isinstance(X, pd.DataFrame):
            df = X.copy()
        else:
            df = pd.DataFrame(X, columns=self.feature_columns)

        df_encoded = self._encode_features(df)

        # Impute missing values with learned medians
        for col in self.feature_columns:
            median = self.median_values_.get(col, 0.0)
            df_encoded[col] = df_encoded[col].fillna(median)

        return df_encoded[self.feature_columns].to_numpy(dtype=np.float64)
