package org.cabbook.support;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "support_tickets")
@Getter @Setter @NoArgsConstructor
public class SupportTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "ride_id")
    private Long rideId;
    private String subject;
    private String message;
    private String status = "OPEN";
    private LocalDateTime createdAt = LocalDateTime.now();
}
