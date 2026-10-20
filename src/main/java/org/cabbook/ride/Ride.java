package org.cabbook.ride;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cabbook.common.ApiException;
import org.cabbook.driver.Driver;
import org.cabbook.user.User;

@Entity
@Table(name = "rides")
@Getter @Setter @NoArgsConstructor
public class Ride {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "rider_id")
    private User rider;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "driver_id")
    private Driver driver;

    private String pickupAddress;
    private double pickupLat;
    private double pickupLng;
    private String dropAddress;
    private double dropLat;
    private double dropLng;

    private BigDecimal distanceKm;
    private int durationMin;
    private BigDecimal fare;
    private BigDecimal discount = BigDecimal.ZERO;
    private String promoCode;

    @Enumerated(EnumType.STRING)
    private RideStatus status = RideStatus.REQUESTED;

    private LocalDateTime requestedAt = LocalDateTime.now();
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    /** Optimistic lock: if two drivers accept at the same instant, the second save fails. */
    @Version
    private long version;

    public BigDecimal getPayable() { return fare.subtract(discount); }

    public void moveTo(RideStatus next) {
        if (!status.canTransitionTo(next))
            throw ApiException.conflict("Cannot change ride from " + status + " to " + next);
        status = next;
        LocalDateTime now = LocalDateTime.now();
        switch (next) {
            case ACCEPTED -> acceptedAt = now;
            case STARTED -> startedAt = now;
            case COMPLETED -> completedAt = now;
            default -> {}
        }
    }
}
