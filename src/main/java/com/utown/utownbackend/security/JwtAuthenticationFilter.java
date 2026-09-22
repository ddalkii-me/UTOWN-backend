package com.utown.utownbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            final String phone = jwtUtil.extractUsername(token);

            if (StringUtils.hasText(phone) && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                if (jwtUtil.validateToken(token)) {
                    // Reject non-access tokens (e.g., password reset tokens)
                    String purpose = jwtUtil.extractClaim(token, claims -> claims.get("purpose", String.class));
                    if (purpose != null) {
                        log.warn("Rejected non-access token with purpose: {}", purpose);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    List<String> roles = jwtUtil.extractRoles(token);
                    List<SimpleGrantedAuthority> authorities = roles != null 
                            ? roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                            : Collections.emptyList();

                    Long userId = jwtUtil.extractClaim(token, claims -> claims.get("userId", Long.class));
                    
                    com.utown.utownbackend.entity.User proxyUser = new com.utown.utownbackend.entity.User();
                    proxyUser.setId(userId);
                    proxyUser.setPhone(phone);
                    if (roles != null && !roles.isEmpty()) {
                        try {
                            String roleStr = roles.get(0).replace("ROLE_", "");
                            proxyUser.setRole(com.utown.utownbackend.entity.UserRole.valueOf(roleStr));
                        } catch (Exception ignored) {
                        }
                    }
                    proxyUser.setStatus(com.utown.utownbackend.entity.UserStatus.ACTIVE);

                    UserDetails userDetails = new CustomUserDetails(proxyUser);

                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        } catch (Exception ex) {
            log.warn("Cannot set user authentication from token: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
