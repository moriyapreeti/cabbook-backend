package org.cabbook.auth;

import jakarta.validation.Valid;
import org.cabbook.auth.AuthDtos.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return service.register(r); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return service.login(r); }
}
