package org.marakas73.common.util;

public enum SupportedTextFileFormats {
    TXT("txt"),
    LOG("log"),
    CSV("csv"),
    JSON("json"),
    XML("xml"),
    MD("md"),
    HTML("html"),
    CSS("css"),
    JS("js"),
    YAML("yaml"),
    PROPERTIES("properties"),
    SQL("sql"),
    TSV("tsv"),
    INI("ini"),
    SH("sh"),
    BAT("bat"),
    PY("py"),
    JAVA("java"),
    KT("kt"),
    PHP("php"),
    RB("rb");

    final String extension;

    SupportedTextFileFormats(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return this.extension;
    }

    public static boolean isTextFile(String fileName) {
        if (fileName == null) {
            return false;
        }

        String lowerCaseName = fileName.toLowerCase();
        for (SupportedTextFileFormats format : values()) {
            if (lowerCaseName.endsWith("." + format.getExtension())) {
                return true;
            }
        }

        return false;
    }
}
