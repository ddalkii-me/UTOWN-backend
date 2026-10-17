package com.utown.utownbackend.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Setter
public class RateLimitingService {

    @Value("${rate-limiting.standard.capacity:100}")
    private int standardCapacity = 100;

    @Value("${rate-limiting.standard.refill-duration-minutes:1}")
    private int standardRefillMinutes = 1;

    @Value("${rate-limiting.auth.capacity:5}")
    private int authCapacity = 5;

    @Value("${rate-limiting.auth.refill-duration-minutes:1}")
    private int authRefillMinutes = 1;

    private final Map<String, Bucket> standardBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    // Global rate limit: standard requests
    public Bucket resolveStandardBucket(String ip) {
        return standardBuckets.computeIfAbsent(ip, this::newStandardBucket);
    }

    // Auth rate limit: login/register requests
    public Bucket resolveAuthBucket(String ip) {
        return authBuckets.computeIfAbsent(ip, this::newAuthBucket);
    }

    private Bucket newStandardBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(standardCapacity, Refill.greedy(standardCapacity, Duration.ofMinutes(standardRefillMinutes))))
                .build();
    }

    private Bucket newAuthBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(authCapacity, Refill.greedy(authCapacity, Duration.ofMinutes(authRefillMinutes))))
                .build();
    }
}
