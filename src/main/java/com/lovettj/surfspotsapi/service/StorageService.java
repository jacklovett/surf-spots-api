package com.lovettj.surfspotsapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final String bucketName;
    private final S3Presigner presigner;

    public StorageService(
            @Value("${app.storage.s3.bucket}") String bucketName,
            @Autowired(required = false) S3Presigner presigner) {
        this.bucketName = bucketName;
        this.presigner = presigner;
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
            log.warn("Media upload requested but storage is not configured.");
            throw new IllegalStateException("Media storage is not configured.");
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        try {
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .putObjectRequest(putObjectRequest)
                    .build();

            return presigner.presignPutObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL: {}", e.getMessage());
            throw new RuntimeException("Media storage error", e);
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
}
