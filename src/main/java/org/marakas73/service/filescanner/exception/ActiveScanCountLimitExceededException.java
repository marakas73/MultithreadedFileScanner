package org.marakas73.service.filescanner.exception;

public class ActiveScanCountLimitExceededException extends RuntimeException {
    public ActiveScanCountLimitExceededException(String message) {
        super(message);
    }
}
