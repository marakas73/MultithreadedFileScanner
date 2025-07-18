package org.marakas73.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FileScanContext {
    private final ForkJoinPool pool;
    private final CompletableFuture<List<String>> future;
    private final List<String> partial;
    private final AtomicBoolean interrupted;
    private final String cacheKey;

    private long completedAtMillis;

    public FileScanContext(
            ForkJoinPool pool,
            CompletableFuture<List<String>> future,
            List<String> partial,
            AtomicBoolean interrupted,
            String cacheKey
    ) {
        this.pool = pool;
        this.future = future;
        this.partial = partial;
        this.interrupted = interrupted;
        this.cacheKey = cacheKey;
    }

    public ForkJoinPool getPool() {
        return pool;
    }

    public CompletableFuture<List<String>> getFuture() {
        return future;
    }

    public List<String> getPartial() {
        return partial;
    }

    public AtomicBoolean isInterrupted() {
        return interrupted;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public long getCompletedAtMillis() {
        return completedAtMillis;
    }

    public void setCompletedAtMillis(long completedAtMillis) {
        if(this.completedAtMillis != 0) { // Value can be set only once to avoid relevant problems
            throw new IllegalStateException("CompletedAtMillis already set");
        }
        this.completedAtMillis = completedAtMillis;
    }

    public void completedNow() {
        setCompletedAtMillis(System.currentTimeMillis());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FileScanContext) obj;
        return Objects.equals(this.pool, that.pool) &&
                Objects.equals(this.future, that.future) &&
                Objects.equals(this.partial, that.partial) &&
                Objects.equals(this.interrupted, that.interrupted) &&
                Objects.equals(this.cacheKey, that.cacheKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pool, future, partial, interrupted, cacheKey);
    }

    @Override
    public String toString() {
        return "FileScanContext[" +
                "pool=" + pool + "," +
                "future=" + future + "," +
                "partial=" + partial + "," +
                "interrupted=" + interrupted + "," +
                "cacheKey=" + cacheKey + ']';
    }

}
