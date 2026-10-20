package org.cabbook.rating;

import jakarta.validation.Valid;
import org.cabbook.auth.AuthUser;
import org.cabbook.rating.RatingService.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RatingController {
    private final RatingService service;
    public RatingController(RatingService service) { this.service = service; }

    @PostMapping("/rides/{rideId}/rating")
    @PreAuthorize("hasRole('RIDER')")
    public RatingResponse rate(@AuthenticationPrincipal AuthUser u, @PathVariable Long rideId,
                               @Valid @RequestBody RatingRequest r) {
        return service.rate(u, rideId, r);
    }

    @GetMapping("/drivers/{driverId}/ratings")
    public Page<RatingResponse> forDriver(@PathVariable Long driverId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return service.forDriver(driverId, PageRequest.of(page, Math.min(size, 100)));
    }
}
