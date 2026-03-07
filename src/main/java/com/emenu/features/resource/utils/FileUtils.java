package com.emenu.features.resource.utils;

import com.emenu.enums.resource.FileType;

public final class FileUtils {

    private FileUtils() {}

    // ─────────────────────── MIME / EXTENSION ─────────────────────

    public static String mimeTypeFromBase64(String base64) {
        if (base64 != null && base64.startsWith("data:") && base64.contains(";base64,")) {
            return base64.substring(5, base64.indexOf(';'));
        }
        return "application/octet-stream";
    }

    public static String stripBase64Prefix(String base64) {
        if (base64 != null && base64.contains(",")) {
            return base64.substring(base64.indexOf(',') + 1);
        }
        return base64;
    }

    public static String extensionFromMime(String mimeType) {
        if (mimeType == null) return "";
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg"       -> "jpg";
            case "image/png"        -> "png";
            case "image/gif"        -> "gif";
            case "image/webp"       -> "webp";
            case "image/svg+xml"    -> "svg";
            case "application/pdf"  -> "pdf";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.ms-excel" -> "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            case "text/plain"       -> "txt";
            case "video/mp4"        -> "mp4";
            default -> "";
        };
    }

    public static FileType resolveFileType(String mimeType) {
        if (mimeType != null && mimeType.startsWith("image/")) return FileType.IMAGE;
        return FileType.DOCUMENT;
    }

    public static String mimeFromPath(String path) {
        if (path == null) return "application/octet-stream";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".gif"))  return "image/gif";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".pdf"))  return "application/pdf";
        if (path.endsWith(".mp4"))  return "video/mp4";
        return "application/octet-stream";
    }

    // ─────────────────────── PATH ─────────────────────────────────

    public static String buildFilePath(String appName, String date, String filename) {
        return appName + "/" + date + "/" + filename;
    }
}
