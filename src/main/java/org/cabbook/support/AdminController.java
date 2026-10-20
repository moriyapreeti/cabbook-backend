package org.cabbook.support;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.cabbook.driver.DriverRepository;
import org.cabbook.payment.PaymentRepository;
import org.cabbook.ride.RideRepository;
import org.cabbook.user.UserRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final DriverRepository drivers;
    private final RideRepository rides;
    private final PaymentRepository payments;

    public AdminController(UserRepository users, DriverRepository drivers, RideRepository rides, PaymentRepository payments) {
        this.users = users; this.drivers = drivers; this.rides = rides; this.payments = payments;
    }

    /** System activity at a glance. */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        rides.countByStatus().forEach(row -> byStatus.put(String.valueOf(row[0]), (Long) row[1]));
        BigDecimal revenue = payments.totalPaid();
        if (revenue == null) revenue = BigDecimal.ZERO;
        return Map.of("users", users.count(), "drivers", drivers.count(),
                "ridesByStatus", byStatus, "totalRevenue", revenue);
    }
}
