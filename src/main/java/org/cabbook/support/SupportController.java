package org.cabbook.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.cabbook.auth.AuthUser;
import org.cabbook.common.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class SupportController {
    public record TicketRequest(Long rideId, @NotBlank @Size(max = 150) String subject, @NotBlank @Size(max = 2000) String message) {}

    private final SupportTicketRepository repo;
    public SupportController(SupportTicketRepository repo) { this.repo = repo; }

    @PostMapping("/api/support")
    public SupportTicket create(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody TicketRequest r) {
        SupportTicket t = new SupportTicket();
        t.setUserId(u.id());
        t.setRideId(r.rideId());
        t.setSubject(r.subject());
        t.setMessage(r.message());
        return repo.save(t);
    }

    @GetMapping("/api/support/my")
    public List<SupportTicket> mine(@AuthenticationPrincipal AuthUser u) {
        return repo.findByUserIdOrderByCreatedAtDesc(u.id());
    }

    // --- admin (URL prefix /api/admin/** is ADMIN-only in SecurityConfig) ---
    @GetMapping("/api/admin/tickets")
    public Page<SupportTicket> open(@RequestParam(defaultValue = "OPEN") String status,
                                    @RequestParam(defaultValue = "0") int page) {
        return repo.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, 20));
    }

    @PostMapping("/api/admin/tickets/{id}/resolve")
    @Transactional
    public SupportTicket resolve(@PathVariable Long id) {
        SupportTicket t = repo.findById(id).orElseThrow(() -> ApiException.notFound("Ticket not found"));
        t.setStatus("RESOLVED");
        return t;
    }
}
