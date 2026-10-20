package org.cabbook.ride;

import jakarta.validation.Valid;
import java.util.List;
import org.cabbook.auth.AuthUser;
import org.cabbook.ride.RideDtos.*;
import org.cabbook.receipt.ReceiptService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rides")
public class RideController {
    private final RideService service;
    private final ReceiptService receipts;

    public RideController(RideService service, ReceiptService receipts) {
        this.service = service; this.receipts = receipts;
    }

    // ----- rider -----
    @PostMapping("/estimate")
    @PreAuthorize("hasRole('RIDER')")
    public FareEstimate estimate(@Valid @RequestBody BookRideRequest r) { return service.estimate(r); }

    @PostMapping
    @PreAuthorize("hasRole('RIDER')")
    public RideResponse book(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody BookRideRequest r) {
        return service.book(u, r);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('RIDER','DRIVER')")
    public RideResponse cancel(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) { return service.cancel(u, id); }

    // ----- driver -----
    @GetMapping("/requests")
    @PreAuthorize("hasRole('DRIVER')")
    public List<RideResponse> requests(@AuthenticationPrincipal AuthUser u) { return service.openRequests(u); }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse accept(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) { return service.accept(u, id); }

    @PostMapping("/{id}/arrived")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse arrived(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.progress(u, id, RideStatus.ARRIVED);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse start(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.progress(u, id, RideStatus.STARTED);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse complete(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.progress(u, id, RideStatus.COMPLETED);
    }

    // ----- both -----
    @GetMapping("/my")
    public Page<RideResponse> my(@AuthenticationPrincipal AuthUser u,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return service.history(u, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    public RideResponse get(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) { return service.get(u, id); }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        byte[] pdf = receipts.invoiceFor(u, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
