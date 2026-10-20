package org.cabbook.rating;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByRideId(Long rideId);
    Page<Rating> findByDriverIdOrderByCreatedAtDesc(Long driverId, Pageable p);
}
