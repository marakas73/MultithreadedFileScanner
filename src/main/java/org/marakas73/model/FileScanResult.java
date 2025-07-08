package org.marakas73.model;

import java.util.List;

public record FileScanResult(
        String token,
        boolean completed,
        List<String> result
) {
}
