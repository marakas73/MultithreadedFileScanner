package org.marakas73.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scanner")
public class FileScannerProperties {
    private int threadsCount;
    private long streamFileSizeLimit;
    private long maxActiveScans;
    private long bufferedResultTtl;

    public int getThreadsCount() {
        return this.threadsCount;
    }
    public long getStreamFileSizeLimit() {
        return this.streamFileSizeLimit;
    }
    public long getMaxActiveScans() {
        return maxActiveScans;
    }
    public long getBufferedResultTtl() {
        return bufferedResultTtl;
    }

    public void setThreadsCount(int threadsCount) {
        if(threadsCount <= 0) {
            throw new IllegalArgumentException("Threads count must be more than 0");
        }
        if (threadsCount > Runtime.getRuntime().availableProcessors() * 4) {
            throw new IllegalArgumentException("Threads count number way too big: " + threadsCount);
        }

        this.threadsCount = threadsCount;
    }
    public void setStreamFileSizeLimit(long streamFileSizeLimit) {
        long freeMemory = Runtime.getRuntime().freeMemory();
        // Approximately safe memory with think of UTF-16 and overhead
        long safeFileSize = (freeMemory / 3);

        if(streamFileSizeLimit > safeFileSize) {
            throw new IllegalArgumentException("Stream file size limit more than allowed space in memory");
        }

        this.streamFileSizeLimit = streamFileSizeLimit;
    }
    public void setMaxActiveScans(long maxActiveScans) {
        if(maxActiveScans <= 0) {
            throw new IllegalArgumentException("Max active scans count must be more than 0");
        }

        this.maxActiveScans = maxActiveScans;
    }
    public void setBufferedResultTtl(long bufferedResultTtl) {
        if(bufferedResultTtl <= 0) {
            throw new IllegalArgumentException("Buffered result TTL must be more than 0");
        }

        this.bufferedResultTtl = bufferedResultTtl;
    }
}
