package org.marakas73.core;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class FileNamePatternMatcher {
    public boolean matches(String fileName, String pattern) {
        if(fileName == null) {
            return false;
        }
        if(pattern == null || pattern.isBlank()) {
            return true;
        }
        String regex = Pattern.quote(pattern).replace("*", "\\E.*\\Q");
        return fileName.matches(regex);
    }
}
