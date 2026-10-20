package org.cabbook.driver;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cabbook.user.User;

@Entity
@Table(name = "drivers")
@Getter @Setter @NoArgsConstructor
public class Driver {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    private String vehicleModel;
    private String vehicleNumber;
    private boolean available;
    private Double latitude;
    private Double longitude;
    private LocalDateTime locationUpdatedAt;
    private double avgRating;
    private int ratingCount;
}
