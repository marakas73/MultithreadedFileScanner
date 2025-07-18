package org.marakas73.service.filescanner.util;

import org.marakas73.common.cache.CacheSizeEvaluator;
import org.marakas73.model.FileScanRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FileScanCacheUtils {
    private final static Logger log = LoggerFactory.getLogger(FileScanCacheUtils.class);

    private final CacheManager cacheManager;
    private final CacheSizeEvaluator cacheSizeEvaluator;

    public FileScanCacheUtils(CacheManager cacheManager, CacheSizeEvaluator cacheSizeEvaluator) {
        this.cacheManager = cacheManager;
        this.cacheSizeEvaluator = cacheSizeEvaluator;
    }

    public String buildScanCacheKey(FileScanRequest scanRequest) {
        return scanRequest.directoryPath()
                + ":" + scanRequest.depthLimit()
                + ":" + scanRequest.scanFilter();
    }

    /**
     * @return {@code true} if value cached successfully
     */
    public boolean putValueInCache(String key, Object value, String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null && !cacheSizeEvaluator.isTooBig(value)) {
                cache.put(key, value);
                log.info("Cached value in Redis with key: {}", key);
                return true;
            } else {
                log.warn("Cache {} not found or value too big for key {}", cacheName, key);
                return false;
            }
        } catch (RedisSystemException rse) {
            log.warn("Redis error for key {}: {}", key, rse.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Unexpected error caching value for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    public Optional<List<String>> getCachedResult(String key, String cacheName) {
        // Check if result is already cached
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            // Use wrapper to avoid generics problem
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                List<String> cached = (List<String>) wrapper.get();
                if (cached != null) {
                    // Cache exits, return its value
                    return Optional.of(cached);
                }
            }
        }

        // No cached result found
        return Optional.empty();
    }

    public Optional<String> getCachedKeyByToken(String key, String cacheName) {
        // Check if result is already cached
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            // Use wrapper to avoid generics problem
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                String cached = (String) wrapper.get();
                if (cached != null) {
                    // Cache exits, return its value
                    return Optional.of(cached);
                }
            }
        }

        // No cached result found
        return Optional.empty();
    }
}
