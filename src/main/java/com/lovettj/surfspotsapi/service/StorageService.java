package com.lovettj.surfspotsapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Service
public class StorageService {

    private final String bucketName;
    private final String endpoint;
    private final String region;
    private final String accessKey;
    private final String secretKey;

    public StorageService(
            @Value("${app.storage.s3.bucket}") String bucketName,
            @Value("${app.storage.s3.endpoint}") String endpoint,
            @Value("${app.storage.s3.region}") String region,
            @Value("${app.storage.s3.access-key}") String accessKey,
            @Value("${app.storage.s3.secret-key}") String secretKey) {
        this.bucketName = bucketName;
        this.endpoint = endpoint;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * Generates a presigned URL for uploading a file to S3
     * @param key The S3 object key (path) where the file will be stored
     * @param contentType The content type of the file
     * @return The presigned URL that can be used to upload the file
     */
    public String generatePresignedUploadUrl(String key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        // Create presigner with same configuration as S3Client
        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // Required for Scaleway
                        .build())
                .build()) {

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15)) // URL valid for 15 minutes
                    .putObjectRequest(putObjectRequest)
                    .build();

            return presigner.presignPutObject(presignRequest).url().toString();
        }
    }

    /**
     * Generates the S3 object key for a media file
     * @param mediaId The unique media ID
     * @param mediaType The type of media (image/video), can be null to skip type organization
     * @param basePath The base path for the media (e.g., "trips/media" or "surfboards/media")
     * @return The S3 object key
     */
    public String generateMediaKey(String mediaId, String mediaType, String basePath) {
        if (mediaType != null && !mediaType.isEmpty()) {
            return String.format("%s/%s/%s", basePath, mediaType, mediaId);
        }
        return String.format("%s/%s", basePath, mediaId);
    }
}
