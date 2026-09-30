package com.carepath.service.notification.provider;

public class NotificationDeliveryResult {

    private final boolean success;
    private final String providerMessageId;
    private final String errorMessage;
    private final boolean retryable;

    private NotificationDeliveryResult(boolean success, String providerMessageId, String errorMessage, boolean retryable) {
        this.success = success;
        this.providerMessageId = providerMessageId;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
    }

    public static NotificationDeliveryResult success(String providerMessageId) {
        return new NotificationDeliveryResult(true, providerMessageId, null, false);
    }

    public static NotificationDeliveryResult failure(String errorMessage, boolean retryable) {
        return new NotificationDeliveryResult(false, null, errorMessage, retryable);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
