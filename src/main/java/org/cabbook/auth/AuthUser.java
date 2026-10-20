package org.cabbook.auth;

import org.cabbook.user.Role;

/** Principal stored in the SecurityContext after the JWT is validated. */
public record AuthUser(Long id, String email, Role role) {}
