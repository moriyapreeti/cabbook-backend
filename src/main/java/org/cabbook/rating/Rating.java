package org.cabbook.rating;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cabbook.driver.Driver;
import org.cabbook.ride.Ride;

@Entity
@Table(name = "ratings")
@Getter @Setter @NoArgsConstructor
public class Rating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ride_id")
    private Ride ride;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "driver_id")
    private Driver driver;
    private int stars;
    private String comment;
    private LocalDateTime createdAt = LocalDateTime.now();
}
