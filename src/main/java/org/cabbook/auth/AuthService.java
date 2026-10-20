package org.cabbook.auth;

import org.cabbook.auth.AuthDtos.*;
import org.cabbook.common.ApiException;
import org.cabbook.driver.Driver;
import org.cabbook.driver.DriverRepository;
import org.cabbook.user.Role;
import org.cabbook.user.User;
import org.cabbook.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final DriverRepository drivers;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, DriverRepository drivers, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.drivers = drivers; this.encoder = encoder; this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        if (r.role() == Role.ADMIN) throw ApiException.badRequest("Cannot self-register as ADMIN");
        if (users.existsByEmailIgnoreCase(r.email())) throw ApiException.conflict("Email already registered");

        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email().toLowerCase());
        u.setPhone(r.phone());
        u.setPasswordHash(encoder.encode(r.password()));   // BCrypt
        u.setRole(r.role());
        users.save(u);

        if (r.role() == Role.DRIVER) {
            if (r.vehicleModel() == null || r.vehicleNumber() == null)
                throw ApiException.badRequest("vehicleModel and vehicleNumber are required for drivers");
            Driver d = new Driver();
            d.setUser(u);
            d.setVehicleModel(r.vehicleModel());
            d.setVehicleNumber(r.vehicleNumber().toUpperCase());
            drivers.save(d);
        }
        return new AuthResponse(jwt.generate(u), u.getId(), u.getName(), u.getRole());
    }

    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generate(u), u.getId(), u.getName(), u.getRole());
    }
}
