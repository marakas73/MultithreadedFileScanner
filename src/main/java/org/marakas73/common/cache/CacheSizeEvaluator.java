package org.marakas73.common.cache;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("cacheSizeEvaluator")
public class CacheSizeEvaluator {
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)([kKmMgG][bB]?)");

    private final GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

    @Value("${spring.redis.maxmemory}")
    private String maxMemoryProperty;

    private long maxBytes;

    @PostConstruct
    public void init() {
        maxBytes = parseMemorySize(maxMemoryProperty);
    }

    public boolean isTooBig(Object value) {
        try {
            byte[] bytes = serializer.serialize(value);
            return bytes != null && bytes.length > maxBytes;
        } catch (Exception e) {
            return true;
        }
    }

    private long parseMemorySize(String value) {
        String trimmed = value.trim().toLowerCase();
        Matcher matcher = SIZE_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid memory size format: " + value);
        }

        long number = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        return switch (unit.toLowerCase()) {
            case "kb", "k" -> number * 1024L;
            case "mb", "m" -> number * 1024L * 1024L;
            case "gb", "g" -> number * 1024L * 1024L * 1024L;
            default -> throw new IllegalArgumentException("Unknown memory unit: " + unit);
        };
    }
}
