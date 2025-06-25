package org.marakas73.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scanner")
public class FileScannerProperties {
    private int threadsCount;

    public int getThreadsCount() {
        return threadsCount;
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
}
