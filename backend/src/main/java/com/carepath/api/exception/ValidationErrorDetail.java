package com.carepath.api.exception;

public class ValidationErrorDetail {

    private String field;
    private Object rejectedValue;
    private String rule;

    public ValidationErrorDetail() {
    }

    public ValidationErrorDetail(String field, Object rejectedValue, String rule) {
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.rule = rule;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public void setRejectedValue(Object rejectedValue) {
        this.rejectedValue = rejectedValue;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }
}
