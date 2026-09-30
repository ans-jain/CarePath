package com.carepath.service;

import com.carepath.api.exception.InvalidMeasurementException;
import com.carepath.domain.enums.MetricType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PhysiologicalRangeValidator {

    public void validate(MetricType metricType, BigDecimal value) {
        if (metricType == null) {
            throw new InvalidMeasurementException("Metric type cannot be null");
        }
        if (value == null) {
            throw new InvalidMeasurementException("Measurement value cannot be null");
        }

        double val = value.doubleValue();

        switch (metricType) {
            case SYSTOLIC_BP -> {
                // Bounds: [40.0, 300.0] mmHg
                if (val < 40.0 || val > 300.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: Systolic blood pressure must be between 40.0 and 300.0 mmHg (observed: " + val + ")"
                    );
                }
            }
            case DIASTOLIC_BP -> {
                // Bounds: [30.0, 200.0] mmHg
                if (val < 30.0 || val > 200.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: Diastolic blood pressure must be between 30.0 and 200.0 mmHg (observed: " + val + ")"
                    );
                }
            }
            case HEART_RATE -> {
                // Bounds: [20.0, 260.0] bpm
                if (val < 20.0 || val > 260.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: Heart rate must be between 20.0 and 260.0 bpm (observed: " + val + ")"
                    );
                }
            }
            case FASTING_GLUCOSE -> {
                // Bounds: [20.0, 800.0] mg/dL
                if (val < 20.0 || val > 800.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: Fasting glucose must be between 20.0 and 800.0 mg/dL (observed: " + val + ")"
                    );
                }
            }
            case HBA1C -> {
                // Bounds: [3.0, 20.0] %
                if (val < 3.0 || val > 20.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: HbA1c must be between 3.0 and 20.0% (observed: " + val + ")"
                    );
                }
            }
            case CHOLESTEROL_TOTAL -> {
                // Bounds: [50.0, 800.0] mg/dL
                if (val < 50.0 || val > 800.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: Total cholesterol must be between 50.0 and 800.0 mg/dL (observed: " + val + ")"
                    );
                }
            }
            case CHOLESTEROL_HDL -> {
                // Bounds: [5.0, 200.0] mg/dL
                if (val < 5.0 || val > 200.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: HDL cholesterol must be between 5.0 and 200.0 mg/dL (observed: " + val + ")"
                    );
                }
            }
            case CHOLESTEROL_LDL -> {
                // Bounds: [10.0, 600.0] mg/dL
                if (val < 10.0 || val > 600.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: LDL cholesterol must be between 10.0 and 600.0 mg/dL (observed: " + val + ")"
                    );
                }
            }
            case TRIGLYCERIDES -> {
                // Bounds: [10.0, 2000.0] mg/dL
                if (val < 10.0 || val > 2000.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: Triglycerides must be between 10.0 and 2000.0 mg/dL (observed: " + val + ")"
                    );
                }
            }
            case WEIGHT_KG -> {
                // Bounds: [20.0, 450.0] kg
                if (val < 20.0 || val > 450.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: Weight must be between 20.0 and 450.0 kg (observed: " + val + ")"
                    );
                }
            }
            case BMI -> {
                // Bounds: [10.0, 100.0] kg/m^2
                if (val < 10.0 || val > 100.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: BMI must be between 10.0 and 100.0 kg/m^2 (observed: " + val + ")"
                    );
                }
            }
            case SPO2 -> {
                // Bounds: [50.0, 100.0] %
                if (val < 50.0 || val > 100.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: SpO2 must be between 50.0 and 100.0% (observed: " + val + ")"
                    );
                }
            }
            case SLEEP_HOURS -> {
                // Bounds: (0.0, 24.0] hours
                if (val <= 0.0 || val > 24.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: Sleep duration must be between 0.0 and 24.0 hours (observed: " + val + ")"
                    );
                }
            }
            case STEPS -> {
                // Bounds: (0.0, 150000.0] steps
                if (val <= 0.0 || val > 150000.0) {
                    throw new InvalidMeasurementException(
                            "Physiological range violation: Steps must be between 1 and 150,000 (observed: " + val + ")"
                    );
                }
            }
        }
    }

    public String getCanonicalUnit(MetricType metricType) {
        if (metricType == null) return "unit";
        return switch (metricType) {
            case SYSTOLIC_BP, DIASTOLIC_BP -> "mmHg";
            case HEART_RATE -> "bpm";
            case FASTING_GLUCOSE, CHOLESTEROL_TOTAL, CHOLESTEROL_HDL, CHOLESTEROL_LDL, TRIGLYCERIDES -> "mg/dL";
            case HBA1C, SPO2 -> "%";
            case WEIGHT_KG -> "kg";
            case BMI -> "kg/m^2";
            case SLEEP_HOURS -> "hours";
            case STEPS -> "steps";
        };
    }
}
