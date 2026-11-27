package com.lighttoolbox.modules.job;

import com.lighttoolbox.common.BusinessException;
import com.lighttoolbox.common.ErrorCode;
import com.lighttoolbox.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 按用户日限流。
 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    public int remaining(Long userId) {
        String key = key(userId);
        String val = redisTemplate.opsForValue().get(key);
        int used = val == null ? 0 : Integer.parseInt(val);
        return Math.max(0, appProperties.getQuota().getDailyLimit() - used);
    }

    public void consume(Long userId) {
        String key = key(userId);
        Long used = redisTemplate.opsForValue().increment(key);
        if (used != null && used == 1L) {
            redisTemplate.expire(key, Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atTime(LocalTime.MAX)));
        }
        if (used != null && used > appProperties.getQuota().getDailyLimit()) {
            redisTemplate.opsForValue().decrement(key);
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED);
        }
    }

    private String key(Long userId) {
        return "lt:quota:" + LocalDate.now() + ":" + userId;
    }
}
