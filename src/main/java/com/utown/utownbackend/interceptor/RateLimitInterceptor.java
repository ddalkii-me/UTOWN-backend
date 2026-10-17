package com.utown.utownbackend.interceptor;

import com.utown.utownbackend.exception.RateLimitExceededException;
import com.utown.utownbackend.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    public RateLimitInterceptor(@Autowired(required = false) RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Value("${rate-limiting.trusted-proxies:}")
    private List<String> trustedProxies;

    @Value("${rate-limiting.trust-all-proxies:false}")
    private boolean trustAllProxies;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (rateLimitingService == null) {
            return true;
        }
        String clientIp = getClientIP(request);
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 1. Stricter auth rate limit specifically for POST /api/auth/login and POST /api/auth/register
        boolean isAuthEndpoint = "POST".equalsIgnoreCase(method) && 
                (requestURI.equals("/api/auth/login") || requestURI.startsWith("/api/auth/login/") ||
                 requestURI.equals("/api/auth/register") || requestURI.startsWith("/api/auth/register/"));

        if (isAuthEndpoint) {
            Bucket authBucket = rateLimitingService.resolveAuthBucket(clientIp);
            if (!authBucket.tryConsume(1)) {
                throw new RateLimitExceededException("Too many authentication requests. Please try again later.");
            }
            return true;
        }

        // 2. Global limit for standard authenticated API requests
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null 
                && authentication.isAuthenticated() 
                && !(authentication instanceof AnonymousAuthenticationToken);

        if (isAuthenticated) {
            String userKey = authentication.getName() != null ? authentication.getName() : clientIp;
            Bucket standardBucket = rateLimitingService.resolveStandardBucket(userKey);
            if (!standardBucket.tryConsume(1)) {
                throw new RateLimitExceededException("Too many requests. Please try again later.");
            }
        }

        return true;
    }

    public String getClientIP(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustAllProxies || (trustedProxies != null && !trustedProxies.isEmpty() && trustedProxies.contains(remoteAddr))) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.trim().isEmpty()) {
                return xfHeader.split(",")[0].trim();
            }
        }
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public void setTrustAllProxies(boolean trustAllProxies) {
        this.trustAllProxies = trustAllProxies;
    }
}
