package com.utown.utownbackend.interceptor;

import com.utown.utownbackend.exception.RateLimitExceededException;
import com.utown.utownbackend.service.RateLimitingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitInterceptorTest {

    private RateLimitingService rateLimitingService;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
        interceptor = new RateLimitInterceptor(rateLimitingService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Auth endpoint (POST /api/auth/login) - allows 5 requests, blocks 6th with 429 exception")
    void authLogin_rateLimitExceeded_after5Requests() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(request, response, new Object()));
        }

        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> interceptor.preHandle(request, response, new Object())
        );
        assertTrue(ex.getMessage().contains("Too many authentication requests"));
    }

    @Test
    @DisplayName("Auth endpoint (POST /api/auth/register) - allows 5 requests, blocks 6th")
    void authRegister_rateLimitExceeded_after5Requests() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setRemoteAddr("192.168.1.101");
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 5; i++) {
            assertTrue(interceptor.preHandle(request, response, new Object()));
        }

        assertThrows(
                RateLimitExceededException.class,
                () -> interceptor.preHandle(request, response, new Object())
        );
    }

    @Test
    @DisplayName("Public unauthenticated endpoint - does not consume standard authenticated rate limit")
    void publicUnauthenticatedEndpoint_doesNotExceedStandardRateLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/restaurants");
        request.setRemoteAddr("192.168.1.102");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // More than 100 requests should still pass because it is an unauthenticated public endpoint
        for (int i = 0; i < 110; i++) {
            assertTrue(interceptor.preHandle(request, response, new Object()));
        }
    }

    @Test
    @DisplayName("Anonymous authentication token - treated as unauthenticated for public endpoints")
    void anonymousAuthentication_doesNotConsumeStandardRateLimit() {
        AnonymousAuthenticationToken anonToken = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(anonToken);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cities");
        request.setRemoteAddr("192.168.1.103");
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 110; i++) {
            assertTrue(interceptor.preHandle(request, response, new Object()));
        }
    }

    @Test
    @DisplayName("Authenticated standard request - allows 100 requests, blocks 101st")
    void authenticatedStandardRequest_rateLimitExceeded_after100Requests() {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "user@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.setRemoteAddr("192.168.1.104");
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 100; i++) {
            assertTrue(interceptor.preHandle(request, response, new Object()));
        }

        RateLimitExceededException ex = assertThrows(
                RateLimitExceededException.class,
                () -> interceptor.preHandle(request, response, new Object())
        );
        assertTrue(ex.getMessage().contains("Too many requests"));
    }

    @Test
    @DisplayName("Client IP extraction - untrusted proxy ignores X-Forwarded-For to prevent spoofing")
    void getClientIP_untrustedProxy_ignoresXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("203.0.113.195");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");

        String clientIp = interceptor.getClientIP(request);
        assertEquals("203.0.113.195", clientIp);
    }

    @Test
    @DisplayName("Client IP extraction - trusted proxy uses X-Forwarded-For")
    void getClientIP_trustedProxy_usesXForwardedFor() {
        interceptor.setTrustedProxies(List.of("127.0.0.1", "10.0.0.50"));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.50");
        request.addHeader("X-Forwarded-For", "198.51.100.17, 10.0.0.50");

        String clientIp = interceptor.getClientIP(request);
        assertEquals("198.51.100.17", clientIp);
    }

    @Test
    @DisplayName("Client IP extraction - trustAllProxies enabled uses X-Forwarded-For")
    void getClientIP_trustAllProxies_usesXForwardedFor() {
        interceptor.setTrustAllProxies(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("172.16.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.42");

        String clientIp = interceptor.getClientIP(request);
        assertEquals("203.0.113.42", clientIp);
    }
}
