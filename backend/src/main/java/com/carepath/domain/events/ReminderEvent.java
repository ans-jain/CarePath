package com.carepath.domain.events;

import java.util.UUID;

public class ReminderEvent {

    private final UUID userId;
    private final String reminderType;
    private final String title;
    private final String message;
    private final String eventId;

    public ReminderEvent(UUID userId, String reminderType, String title, String message, String eventId) {
        this.userId = userId;
        this.reminderType = reminderType;
        this.title = title;
        this.message = message;
        this.eventId = eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getReminderType() {
        return reminderType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getEventId() {
        return eventId;
    }
}
