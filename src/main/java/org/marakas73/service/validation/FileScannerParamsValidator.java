package org.marakas73.service.validation;

import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class FileScannerParamsValidator {
    public boolean isPathValid(String path) {
        try {
            Paths.get(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
