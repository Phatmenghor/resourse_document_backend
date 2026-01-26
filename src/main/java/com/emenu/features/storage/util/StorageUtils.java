package com.emenu.features.storage.util;

import com.emenu.enums.storage.ResourceType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StorageUtils {

    private static final Map<String, ResourceType> MIME_TYPE_MAPPING = new HashMap<>();
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/svg+xml"
    );
    private static final Set<String> PDF_MIME_TYPES = Set.of(
            "application/pdf"
    );
    private static final Set<String> DOCUMENT_MIME_TYPES = Set.of(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv"
    );
    private static final Set<String> VIDEO_MIME_TYPES = Set.of(
            "video/mp4", "video/mpeg", "video/webm", "video/quicktime", "video/x-msvideo"
    );
    private static final Set<String> AUDIO_MIME_TYPES = Set.of(
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/webm", "audio/aac"
    );

    /**
     * Determine ResourceType from MIME type
     */
    public static ResourceType getResourceType(String mimeType) {
        if (mimeType == null) {
            return ResourceType.OTHER;
        }

        String lowerMimeType = mimeType.toLowerCase();

        if (IMAGE_MIME_TYPES.contains(lowerMimeType) || lowerMimeType.startsWith("image/")) {
            return ResourceType.IMAGE;
        }
        if (PDF_MIME_TYPES.contains(lowerMimeType)) {
            return ResourceType.PDF;
        }
        if (DOCUMENT_MIME_TYPES.contains(lowerMimeType)) {
            return ResourceType.DOCUMENT;
        }
        if (VIDEO_MIME_TYPES.contains(lowerMimeType) || lowerMimeType.startsWith("video/")) {
            return ResourceType.VIDEO;
        }
        if (AUDIO_MIME_TYPES.contains(lowerMimeType) || lowerMimeType.startsWith("audio/")) {
            return ResourceType.AUDIO;
        }

        return ResourceType.OTHER;
    }

    /**
     * Generate a unique stored filename using UUID
     */
    public static String generateStoredFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }

    /**
     * Extract file extension from filename
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * Sanitize storage path name from API key name
     */
    public static String sanitizePathName(String name) {
        if (name == null || name.isEmpty()) {
            return "default";
        }
        // Replace spaces and special characters with underscores
        return name.toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    /**
     * Build storage path from API key path and filename
     */
    public static String buildStoragePath(String apiKeyPath, String storedFilename) {
        return apiKeyPath + "/" + storedFilename;
    }

    /**
     * Format bytes to human readable string
     */
    public static String formatBytes(long bytes) {
        if (bytes == 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * Validate file size against maximum allowed
     */
    public static boolean isFileSizeValid(long fileSizeBytes, long maxFileSizeMb) {
        long maxBytes = maxFileSizeMb * 1024 * 1024;
        return fileSizeBytes <= maxBytes;
    }

    /**
     * Check if file type is allowed
     */
    public static boolean isFileTypeAllowed(ResourceType resourceType, String allowedTypes) {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return true; // All types allowed
        }

        String[] allowed = allowedTypes.split(",");
        for (String type : allowed) {
            if (type.trim().equalsIgnoreCase(resourceType.name())) {
                return true;
            }
        }
        return false;
    }
}
