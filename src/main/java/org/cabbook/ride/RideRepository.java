package org.cabbook.ride;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RideRepository extends JpaRepository<Ride, Long> {

    @Query(value = "select r from Ride r join fetch r.rider left join fetch r.driver d left join fetch d.user where r.rider.id = :uid order by r.requestedAt desc",
           countQuery = "select count(r) from Ride r where r.rider.id = :uid")
    Page<Ride> findByRider(@Param("uid") Long riderUserId, Pageable p);

    @Query(value = "select r from Ride r join fetch r.rider left join fetch r.driver d left join fetch d.user where d.id = :did order by r.requestedAt desc",
           countQuery = "select count(r) from Ride r where r.driver.id = :did")
    Page<Ride> findByDriver(@Param("did") Long driverId, Pageable p);

    @Query("select r from Ride r join fetch r.rider left join fetch r.driver d left join fetch d.user where r.id = :id")
    Optional<Ride> findDetailed(@Param("id") Long id);

    @Query("select r from Ride r where r.driver.id = :did and r.status in (org.cabbook.ride.RideStatus.ACCEPTED, org.cabbook.ride.RideStatus.ARRIVED, org.cabbook.ride.RideStatus.STARTED)")
    Optional<Ride> findActiveByDriver(@Param("did") Long driverId);

    boolean existsByRiderIdAndStatusIn(Long riderId, Collection<RideStatus> statuses);

    /** Open requests inside a bounding box around the driver (cheap pre-filter). */
    @Query("select r from Ride r join fetch r.rider where r.status = org.cabbook.ride.RideStatus.REQUESTED " +
           "and r.pickupLat between :minLat and :maxLat and r.pickupLng between :minLng and :maxLng order by r.requestedAt")
    List<Ride> findOpenRequestsInBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                     @Param("minLng") double minLng, @Param("maxLng") double maxLng);

    @Query("select sum(r.fare - r.discount) from Ride r where r.driver.id = :did and r.status = :status")
    BigDecimal sumFareByDriverAndStatus(@Param("did") Long driverId, @Param("status") RideStatus status);

    long countByDriverIdAndStatus(Long driverId, RideStatus status);

    @Query("select count(r) > 0 from Ride r left join r.driver d where r.id = :id and (r.rider.id = :uid or d.user.id = :uid)")
    boolean isParticipant(@Param("id") Long rideId, @Param("uid") Long userId);

    @Query("select r.status, count(r) from Ride r group by r.status")
    List<Object[]> countByStatus();
}
