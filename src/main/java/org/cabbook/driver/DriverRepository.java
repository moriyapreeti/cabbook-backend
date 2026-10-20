package org.cabbook.driver;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Query("select d from Driver d join fetch d.user where d.user.id = :userId")
    Optional<Driver> findByUserId(@Param("userId") Long userId);

    /**
     * Nearby available drivers using the Haversine formula (works on plain PostgreSQL, no PostGIS needed).
     * Only drivers whose location was updated in the last 5 minutes are considered.
     * Swap this for PostGIS ST_DWithin or Redis GEOSEARCH when you need to scale.
     */
    @Query(value = """
        SELECT d.* FROM (
          SELECT dr.*, (6371 * acos(least(1.0,
              cos(radians(:lat)) * cos(radians(dr.latitude)) * cos(radians(dr.longitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(dr.latitude))))) AS dist_km
          FROM drivers dr
          WHERE dr.available = TRUE AND dr.latitude IS NOT NULL
            AND dr.location_updated_at > now() - interval '5 minutes'
        ) d
        WHERE d.dist_km <= :radiusKm
        ORDER BY d.dist_km ASC
        """, nativeQuery = true)
    List<Driver> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                            @Param("radiusKm") double radiusKm, Pageable pageable);

    @Query("select d from Driver d join fetch d.user where d.ratingCount > 0 order by d.avgRating desc, d.ratingCount desc")
    List<Driver> findTopRated(Pageable pageable);

    /** Atomic running average, safe even if two ratings arrive at the same time. */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("update Driver d set d.avgRating = (d.avgRating * d.ratingCount + :stars) / (d.ratingCount + 1), d.ratingCount = d.ratingCount + 1 where d.id = :id")
    void addRating(@Param("id") Long id, @Param("stars") double stars);
}
