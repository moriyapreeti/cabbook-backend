package org.cabbook.driver;

import jakarta.validation.Valid;
import java.util.List;
import org.cabbook.auth.AuthUser;
import org.cabbook.driver.DriverDtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {
    private final DriverService service;
    public DriverController(DriverService service) { this.service = service; }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverProfile me(@AuthenticationPrincipal AuthUser u) { return service.profile(u.id()); }

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public DriverProfile availability(@AuthenticationPrincipal AuthUser u, @RequestBody AvailabilityUpdate r) {
        return service.setAvailability(u.id(), r.available());
    }

    /** Driver app calls this every 3-5 seconds. */
    @PutMapping("/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    public void location(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody LocationUpdate r) {
        service.updateLocation(u.id(), r);
    }

    @GetMapping("/me/earnings")
    @PreAuthorize("hasRole('DRIVER')")
    public Earnings earnings(@AuthenticationPrincipal AuthUser u) { return service.earnings(u.id()); }

    /** Driver ranking by rating. */
    @GetMapping("/top")
    public List<DriverProfile> top(@RequestParam(defaultValue = "10") int limit) {
        return service.top(PageRequest.of(0, Math.min(limit, 50)));
    }
}
