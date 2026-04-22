package com.lovettj.surfspotsapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);
    private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(15);
    private static final Duration DOWNLOAD_URL_DURATION = Duration.ofHours(2);

    private final String bucketName;
    private final S3Presigner presigner;
    private final S3Client s3Client;

    @Autowired
    public StorageService(
            @Value("${app.storage.s3.bucket}") String bucketName,
            @Autowired(required = false) S3Presigner presigner,
            @Autowired(required = false) S3Client s3Client) {
        this.bucketName = bucketName;
        this.presigner = presigner;
        this.s3Client = s3Client;
    }

    StorageService(String bucketName, S3Presigner presigner) {
        this(bucketName, presigner, null);
    }

    /**
     * Returns true if storage is configured (presigner bean available). When false, media upload will fail.
     */
    public boolean isStorageConfigured() {
        return presigner != null;
    }

    /**
     * Generates a presigned URL for uploading a file to object storage.
     * @param key The object key (path) where the file will be stored
     * @param contentType The content type of the file
     * @return The presigned URL that can be used to upload the file
     * @throws IllegalStateException if storage is not configured
     * @throws RuntimeException if presigning fails
     */
    public String generatePresignedUploadUrl(String key, String contentType) {
        if (!isStorageConfigured()) {
            throw new IllegalStateException("Media storage is not configured.");
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        try {
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(UPLOAD_URL_DURATION)
                    .putObjectRequest(putObjectRequest)
                    .build();

            return presigner.presignPutObject(presignRequest).url().toString();
        } catch (Exception exception) {
            throw new RuntimeException("Media storage error", exception);
        }
    }


    /**
     * Generates the storage object key for a media file.
     * @param mediaId The unique media ID
     * @param mediaType The type of media (image/video), can be null to skip type organization
     * @param basePath The base path for the media (e.g. "trips/media" or "surfboards/media")
     * @return The object key
     */
    public String generateMediaKey(String mediaId, String mediaType, String basePath) {
        if (mediaType != null && !mediaType.isEmpty()) {
            return String.format("%s/%s/%s", basePath, mediaType, mediaId);
        }
        return String.format("%s/%s", basePath, mediaId);
    }

    /**
     * Generates a presigned URL for downloading a file from object storage.
     */
    public String generatePresignedDownloadUrl(String key) {
        if (!isStorageConfigured()) {
            throw new IllegalStateException("Media storage is not configured.");
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(DOWNLOAD_URL_DURATION)
                    .getObjectRequest(getObjectRequest)
                    .build();
            return presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception exception) {
            throw new RuntimeException("Media storage error", exception);
        }
    }

    /**
     * Deletes a storage object when a key is provided.
     * Returns false when key is missing or storage is not configured for delete.
     */
    public boolean deleteObject(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        if (s3Client == null) {
            logger.warn("Media delete requested but S3 client is not configured.");
            return false;
        }
        
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (Exception exception) {
            logger.warn("Failed to delete object from storage. key={}", key, exception);
            return false;
        }
    }

    /**
     * Resolves the storage key from a stored key first, then falls back to parsing a URL.
     */
    public String resolveObjectKey(String storedObjectKey, String fallbackUrl) {
        if (storedObjectKey != null && !storedObjectKey.isBlank()) {
            return storedObjectKey;
        }
        return extractObjectKeyFromUrl(fallbackUrl);
    }

    /**
     * Resolves an object key from stored key or URL and falls back to a generated media key.
     */
    public String resolveObjectKeyWithFallback(
            String storedObjectKey,
            String fallbackUrl,
            String mediaId,
            String mediaType,
            String basePath) {
        String resolvedKey = resolveObjectKey(storedObjectKey, fallbackUrl);
        if (resolvedKey != null && !resolvedKey.isBlank()) {
            return resolvedKey;
        }
        return generateMediaKey(mediaId, mediaType, basePath);
    }

    /**
     * Extracts an object key from path-style or virtual-host-style object URLs.
     */
    public String extractObjectKeyFromUrl(String objectUrl) {
        if (objectUrl == null || objectUrl.isBlank()) {
            return null;
        }
        
        try {
            URI uri = URI.create(objectUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }

            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
            if (normalizedPath.startsWith(bucketName + "/")) {
                normalizedPath = normalizedPath.substring(bucketName.length() + 1);
            } else if (normalizedPath.equals(bucketName)) {
                return null;
            }

            if (normalizedPath.isBlank()) {
                return null;
            }
            return URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            logger.warn("Could not parse object key from URL: {}", objectUrl, exception);
            return null;
        }
    }
}
