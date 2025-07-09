package org.marakas73.service.filescanner.util;

import org.marakas73.common.cache.CacheSizeEvaluator;
import org.marakas73.model.FileScanRequest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FileScanCacheUtils {
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
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null && !cacheSizeEvaluator.isTooBig(value)) {
            // Cache
            cache.put(key, value);
            // TODO: Cache log here
            return true;
        }

        return false;
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
                    // TODO: Cache log here
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
                    // TODO: Cache log here
                    return Optional.of(cached);
                }
            }
        }

        // No cached result found
        return Optional.empty();
    }
}
