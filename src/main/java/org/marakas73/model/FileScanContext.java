package org.marakas73.model;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;

public record FileScanContext(
        ForkJoinPool pool,
        ForkJoinTask<List<String>> future,
        List<String> partial,
        AtomicBoolean interrupted,
        String cacheKey
) {
}
