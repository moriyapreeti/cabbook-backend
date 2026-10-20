package org.cabbook.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Simple in-memory brute-force protection: 10 auth requests per minute per IP. Use Redis if you scale to many instances. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 10;
    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !req.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        int n = hits.computeIfAbsent(req.getRemoteAddr(), k -> new AtomicInteger()).incrementAndGet();
        if (n > LIMIT) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Too many requests, try again in a minute\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    @Scheduled(fixedRate = 60_000)
    public void reset() { hits.clear(); }
}
