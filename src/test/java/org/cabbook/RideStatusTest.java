package org.cabbook;

import static org.junit.jupiter.api.Assertions.*;

import org.cabbook.common.ApiException;
import org.cabbook.ride.Ride;
import org.cabbook.ride.RideStatus;
import org.junit.jupiter.api.Test;

class RideStatusTest {
    @Test
    void happyPath() {
        assertTrue(RideStatus.REQUESTED.canTransitionTo(RideStatus.ACCEPTED));
        assertTrue(RideStatus.ACCEPTED.canTransitionTo(RideStatus.ARRIVED));
        assertTrue(RideStatus.ARRIVED.canTransitionTo(RideStatus.STARTED));
        assertTrue(RideStatus.STARTED.canTransitionTo(RideStatus.COMPLETED));
    }

    @Test
    void illegalTransitions() {
        assertFalse(RideStatus.COMPLETED.canTransitionTo(RideStatus.REQUESTED));
        assertFalse(RideStatus.REQUESTED.canTransitionTo(RideStatus.STARTED));
        assertFalse(RideStatus.STARTED.canTransitionTo(RideStatus.CANCELLED));
        assertFalse(RideStatus.CANCELLED.canTransitionTo(RideStatus.ACCEPTED));
    }

    @Test
    void rideRejectsInvalidMove() {
        Ride r = new Ride();
        r.moveTo(RideStatus.ACCEPTED);
        assertThrows(ApiException.class, () -> r.moveTo(RideStatus.COMPLETED));
        assertNotNull(r.getAcceptedAt());
    }
}
