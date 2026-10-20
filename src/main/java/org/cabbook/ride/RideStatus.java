package org.cabbook.ride;

/** The single place that defines which ride status changes are legal. */
public enum RideStatus {
    REQUESTED, ACCEPTED, ARRIVED, STARTED, COMPLETED, CANCELLED;

    public boolean canTransitionTo(RideStatus next) {
        return switch (this) {
            case REQUESTED -> next == ACCEPTED || next == CANCELLED;
            case ACCEPTED -> next == ARRIVED || next == CANCELLED;
            case ARRIVED -> next == STARTED || next == CANCELLED;
            case STARTED -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean isActive() { return this == ACCEPTED || this == ARRIVED || this == STARTED; }
}
