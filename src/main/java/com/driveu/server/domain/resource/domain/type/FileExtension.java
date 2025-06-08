package com.driveu.server.domain.resource.domain.type;

import java.util.Locale;

public enum FileExtension {
    TXT, PDF, MD, DOC, DOCX, PNG, JPEG, JPG;

    public String getContentType() {
        return switch (this) {
            case TXT -> "text/plain";
            case PDF -> "application/pdf";
            case MD -> "text/markdown";
            case DOC -> "application/msword";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case PNG -> "image/png";
            case JPEG, JPG -> "image/jpeg";
        };
    }

    public static FileExtension fromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name: " + filename);
        }

        String ext = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);

        try {
            return FileExtension.valueOf(ext);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported file extension: " + ext);
        }
    }
}
