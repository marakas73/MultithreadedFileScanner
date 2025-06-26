package org.marakas73.model;

import jakarta.annotation.Nullable;

public record FileScanRequest(
        String directoryPath,
        @Nullable Integer threadsCount,
        @Nullable FileScanFilter scanFilter
) {
}
