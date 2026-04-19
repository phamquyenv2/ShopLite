package com.quyen.shoplite.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event published after an Order-related transaction commits successfully.
 * Listeners (e.g. FCM push notification) run AFTER the DB commit,
 * so a notification is never sent for data that wasn't persisted.
 */
@Getter
@AllArgsConstructor
public class OrderCompletedEvent {
    private final Order order;
}
