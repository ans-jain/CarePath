import pytest
import numpy as np

from app.models.feature_vector import FeatureVector, FEATURE_COLUMNS
from app.services.model_registry import model_registry
from app.services.shap_explainer import shap_explainer

@pytest.fixture(scope="module", autouse=True)
def ensure_model_loaded():
    if not model_registry.is_loaded:
        from app.training.train import train_and_persist_model
        train_and_persist_model(n_samples=500, random_seed=42)
        model_registry.load()

@pytest.fixture
def sample_feature_vector():
    return FeatureVector(
        age=58.0,
        biological_sex="MALE",
        bmi=31.2,
        smoking_status="CURRENT",
        systolic_bp_current=148.0,
        diastolic_bp_current=92.0,
        heart_rate_current=84.0,
        fasting_glucose_current=135.0,
        hba1c_current=6.8,
        cholesterol_total=235.0,
        cholesterol_hdl=38.0,
        cholesterol_ldl=155.0,
        triglycerides=210.0,
        sleep_hours_current=5.5,
        systolic_bp_ewma_delta=8.5,
        fasting_glucose_ewma_delta=12.0,
        systolic_bp_slope_14d=0.75,
        fasting_glucose_slope_14d=0.90
    )

def test_shap_explainer_initialization():
    """Verify that TreeExplainer initializes across ensemble folds."""
    shap_explainer._initialize_explainers()
    assert shap_explainer._explainers is not None
    assert len(shap_explainer._explainers) >= 1
    assert shap_explainer._base_value is not None
    assert isinstance(shap_explainer._base_value, float)

def test_shap_explanation_shape_and_keys(sample_feature_vector):
    """Verify that SHAP explanation produces exactly 18 feature values."""
    X_proc = model_registry.preprocessor.transform(sample_feature_vector)
    result = shap_explainer.explain(X_proc, sample_feature_vector, top_k=5)

    assert result.base_value is not None
    assert len(result.shap_values) == 18
    for col in FEATURE_COLUMNS:
        assert col in result.shap_values
        assert isinstance(result.shap_values[col], float)

def test_shap_direction_and_classification(sample_feature_vector):
    """Verify that positive and negative contributions are properly segregated."""
    X_proc = model_registry.preprocessor.transform(sample_feature_vector)
    result = shap_explainer.explain(X_proc, sample_feature_vector, top_k=5)

    for driver in result.top_risk_drivers:
        assert driver.direction == "increases_risk"
        assert driver.shap_value > 0
        assert driver.display_name is not None
        assert driver.narrative is not None
        assert driver.impact in ["low", "medium", "high"]

    for factor in result.protective_factors:
        assert factor.direction == "decreases_risk"
        assert factor.shap_value < 0
        assert factor.display_name is not None
        assert factor.narrative is not None

def test_shap_top_k_ordering(sample_feature_vector):
    """Verify that top-k features are ordered by absolute contribution magnitude."""
    X_proc = model_registry.preprocessor.transform(sample_feature_vector)
    top_k = 3
    result = shap_explainer.explain(X_proc, sample_feature_vector, top_k=top_k)

    assert len(result.top_risk_drivers) <= top_k
    assert len(result.protective_factors) <= top_k

    if len(result.top_risk_drivers) > 1:
        for i in range(len(result.top_risk_drivers) - 1):
            assert abs(result.top_risk_drivers[i].shap_value) >= abs(result.top_risk_drivers[i + 1].shap_value)

def test_shap_local_additivity_property(sample_feature_vector):
    """Verify that sum of SHAP values + base value matches model tree decision output."""
    X_proc = model_registry.preprocessor.transform(sample_feature_vector)
    shap_explainer._initialize_explainers()

    # For each individual fold explainer, test additivity
    model = model_registry.model
    if hasattr(model, "calibrated_classifiers_"):
        for cc, exp in zip(model.calibrated_classifiers_, shap_explainer._explainers):
            est = cc.estimator
            sv = exp.shap_values(X_proc)
            bv = exp.expected_value
            if isinstance(bv, (list, np.ndarray)):
                bv = float(bv[0])
            pred_margin = float(est.decision_function(X_proc)[0])
            sum_shap = float(bv + np.sum(sv[0]))
            assert np.isclose(pred_margin, sum_shap, atol=1e-4)

def test_shap_unloaded_model_raises_error():
    """Verify that calling explain when model is unloaded raises RuntimeError."""
    model_registry.unload()
    shap_explainer.clear_cache()

    with pytest.raises(RuntimeError, match="Model is not loaded"):
        shap_explainer._initialize_explainers()

    # Restore model
    model_registry.load()
