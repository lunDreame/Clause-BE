package com.clause.app.common;

import com.clause.app.common.ClauseException;
import com.clause.app.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitGuard {

    private final int perMinute;
    private final Map<String, RateLimitRecord> records = new ConcurrentHashMap<>();

    public RateLimitGuard(@Value("${clause.ratelimit.per-minute:30}") int perMinute) {
        this.perMinute = perMinute;
    }

    public void check(String identifier) {
        Instant now = Instant.now();
        RateLimitRecord record = records.computeIfAbsent(identifier, k -> new RateLimitRecord());

        // 1분 이상 지난 요청 제거
        record.cleanup(now.minusSeconds(60));

        if (record.count >= perMinute) {
            log.warn("Rate limit exceeded for identifier: {}", identifier);
            throw new ClauseException(ErrorCode.RATE_LIMITED);
        }

        record.addRequest(now);
    }

    private static class RateLimitRecord {
        private final Map<Instant, Integer> requests = new ConcurrentHashMap<>();
        private int count = 0;

        synchronized void addRequest(Instant timestamp) {
            requests.put(timestamp, 1);
            count++;
        }

        synchronized void cleanup(Instant cutoff) {
            requests.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoff));
            count = requests.size();
        }
    }
}

