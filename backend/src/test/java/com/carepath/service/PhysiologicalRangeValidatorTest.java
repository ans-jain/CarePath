package com.carepath.service;

import com.carepath.api.exception.InvalidMeasurementException;
import com.carepath.domain.enums.MetricType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhysiologicalRangeValidatorTest {

    private PhysiologicalRangeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PhysiologicalRangeValidator();
    }

    @ParameterizedTest
    @CsvSource({
            "SYSTOLIC_BP, 120.0",
            "SYSTOLIC_BP, 40.0",
            "SYSTOLIC_BP, 300.0",
            "DIASTOLIC_BP, 80.0",
            "DIASTOLIC_BP, 30.0",
            "DIASTOLIC_BP, 200.0",
            "HEART_RATE, 60.0",
            "HEART_RATE, 20.0",
            "HEART_RATE, 260.0",
            "FASTING_GLUCOSE, 95.0",
            "FASTING_GLUCOSE, 20.0",
            "FASTING_GLUCOSE, 800.0",
            "HBA1C, 5.5",
            "HBA1C, 3.0",
            "HBA1C, 20.0",
            "CHOLESTEROL_TOTAL, 190.0",
            "CHOLESTEROL_TOTAL, 50.0",
            "CHOLESTEROL_TOTAL, 800.0",
            "CHOLESTEROL_HDL, 50.0",
            "CHOLESTEROL_HDL, 5.0",
            "CHOLESTEROL_HDL, 200.0",
            "CHOLESTEROL_LDL, 100.0",
            "CHOLESTEROL_LDL, 10.0",
            "CHOLESTEROL_LDL, 600.0",
            "TRIGLYCERIDES, 150.0",
            "TRIGLYCERIDES, 10.0",
            "TRIGLYCERIDES, 2000.0",
            "WEIGHT_KG, 70.0",
            "WEIGHT_KG, 20.0",
            "WEIGHT_KG, 450.0",
            "BMI, 24.5",
            "BMI, 10.0",
            "BMI, 100.0",
            "SPO2, 98.0",
            "SPO2, 50.0",
            "SPO2, 100.0",
            "SLEEP_HOURS, 7.5",
            "SLEEP_HOURS, 0.5",
            "SLEEP_HOURS, 24.0",
            "STEPS, 8000.0",
            "STEPS, 1.0",
            "STEPS, 150000.0"
    })
    @DisplayName("Should accept valid physiological values on and within boundaries")
    void testValidRanges(MetricType type, BigDecimal value) {
        assertThatCode(() -> validator.validate(type, value)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "SYSTOLIC_BP, 39.9",
            "SYSTOLIC_BP, 300.1",
            "DIASTOLIC_BP, 29.9",
            "DIASTOLIC_BP, 200.1",
            "HEART_RATE, 19.9",
            "HEART_RATE, 260.1",
            "FASTING_GLUCOSE, 19.9",
            "FASTING_GLUCOSE, 800.1",
            "HBA1C, 2.9",
            "HBA1C, 20.1",
            "CHOLESTEROL_TOTAL, 49.9",
            "CHOLESTEROL_TOTAL, 800.1",
            "CHOLESTEROL_HDL, 4.9",
            "CHOLESTEROL_HDL, 200.1",
            "CHOLESTEROL_LDL, 9.9",
            "CHOLESTEROL_LDL, 600.1",
            "TRIGLYCERIDES, 9.9",
            "TRIGLYCERIDES, 2000.1",
            "WEIGHT_KG, 19.9",
            "WEIGHT_KG, 450.1",
            "BMI, 9.9",
            "BMI, 100.1",
            "SPO2, 49.9",
            "SPO2, 100.1",
            "SLEEP_HOURS, 0.0",
            "SLEEP_HOURS, 24.1",
            "STEPS, 0.0",
            "STEPS, 150000.1"
    })
    @DisplayName("Should reject physiologically impossible values with InvalidMeasurementException")
    void testInvalidRanges(MetricType type, BigDecimal value) {
        assertThatThrownBy(() -> validator.validate(type, value))
                .isInstanceOf(InvalidMeasurementException.class)
                .hasMessageContaining("Physiological range violation");
    }

    @Test
    @DisplayName("Should verify canonical units for all metrics")
    void testCanonicalUnits() {
        assertThat(validator.getCanonicalUnit(MetricType.SYSTOLIC_BP)).isEqualTo("mmHg");
        assertThat(validator.getCanonicalUnit(MetricType.DIASTOLIC_BP)).isEqualTo("mmHg");
        assertThat(validator.getCanonicalUnit(MetricType.HEART_RATE)).isEqualTo("bpm");
        assertThat(validator.getCanonicalUnit(MetricType.FASTING_GLUCOSE)).isEqualTo("mg/dL");
        assertThat(validator.getCanonicalUnit(MetricType.HBA1C)).isEqualTo("%");
        assertThat(validator.getCanonicalUnit(MetricType.CHOLESTEROL_TOTAL)).isEqualTo("mg/dL");
        assertThat(validator.getCanonicalUnit(MetricType.CHOLESTEROL_HDL)).isEqualTo("mg/dL");
        assertThat(validator.getCanonicalUnit(MetricType.CHOLESTEROL_LDL)).isEqualTo("mg/dL");
        assertThat(validator.getCanonicalUnit(MetricType.TRIGLYCERIDES)).isEqualTo("mg/dL");
        assertThat(validator.getCanonicalUnit(MetricType.WEIGHT_KG)).isEqualTo("kg");
        assertThat(validator.getCanonicalUnit(MetricType.BMI)).isEqualTo("kg/m^2");
        assertThat(validator.getCanonicalUnit(MetricType.SPO2)).isEqualTo("%");
        assertThat(validator.getCanonicalUnit(MetricType.SLEEP_HOURS)).isEqualTo("hours");
        assertThat(validator.getCanonicalUnit(MetricType.STEPS)).isEqualTo("steps");
    }
}
