package com.lovettj.surfspotsapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceTests {

    private static final String BUCKET = "test-bucket";

    @Mock
    private S3Presigner presigner;

    @Mock
    private S3Client s3Client;

    @Test
    void isStorageConfigured_returnsFalse_whenPresignerIsNull() {
        StorageService service = new StorageService(BUCKET, null);
        assertFalse(service.isStorageConfigured());
    }

    @Test
    void isStorageConfigured_returnsTrue_whenPresignerIsSet() {
        StorageService service = new StorageService(BUCKET, presigner);
        assertTrue(service.isStorageConfigured());
    }

    @Test
    void generatePresignedUploadUrl_throwsIllegalStateException_whenNotConfigured() {
        StorageService service = new StorageService(BUCKET, null);
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> service.generatePresignedUploadUrl("key", "image/jpeg"));
        assertTrue(thrown.getMessage().contains("not configured"));
    }

    @Test
    void generatePresignedUploadUrl_returnsUrl_whenConfigured() throws MalformedURLException {
        StorageService service = new StorageService(BUCKET, presigner);
        String expectedUrl = "https://test-bucket.s3.example.com/key";
        PresignedPutObjectRequest response = mock(PresignedPutObjectRequest.class);
        when(response.url()).thenReturn(new URL(expectedUrl));
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(response);

        String url = service.generatePresignedUploadUrl("key", "image/jpeg");

        assertEquals(expectedUrl, url);
    }

    @Test
    void generatePresignedDownloadUrl_returnsUrl_whenConfigured() throws MalformedURLException {
        StorageService service = new StorageService(BUCKET, presigner);
        String expectedUrl = "https://test-bucket.s3.example.com/key";
        PresignedGetObjectRequest response = mock(PresignedGetObjectRequest.class);
        when(response.url()).thenReturn(new URL(expectedUrl));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(response);

        String url = service.generatePresignedDownloadUrl("key");

        assertEquals(expectedUrl, url);
    }

    @Test
    void generateMediaKey_includesMediaType_whenMediaTypeProvided() {
        StorageService service = new StorageService(BUCKET, null);
        String key = service.generateMediaKey("media-123", "image", "trips/media");
        assertEquals("trips/media/image/media-123", key);
    }

    @Test
    void generateMediaKey_omitsMediaType_whenMediaTypeIsNull() {
        StorageService service = new StorageService(BUCKET, null);
        String key = service.generateMediaKey("media-123", null, "trips/media");
        assertEquals("trips/media/media-123", key);
    }

    @Test
    void generateMediaKey_omitsMediaType_whenMediaTypeIsEmpty() {
        StorageService service = new StorageService(BUCKET, null);
        String key = service.generateMediaKey("media-123", "", "surfboards/media");
        assertEquals("surfboards/media/media-123", key);
    }

    @Test
    void extractObjectKeyFromUrl_returnsKey_forPathStyleUrl() {
        StorageService service = new StorageService(BUCKET, null);
        String key = service.extractObjectKeyFromUrl("https://s3.fr-par.scw.cloud/test-bucket/trips/media/image/abc-123");
        assertEquals("trips/media/image/abc-123", key);
    }

    @Test
    void extractObjectKeyFromUrl_returnsKey_forVirtualHostStyleUrl() {
        StorageService service = new StorageService(BUCKET, null);
        String key = service.extractObjectKeyFromUrl("https://test-bucket.s3.fr-par.scw.cloud/trips/media/image/abc-123");
        assertEquals("trips/media/image/abc-123", key);
    }

    @Test
    void resolveObjectKeyWithFallback_returnsStoredKey_whenStoredKeyPresent() {
        StorageService service = new StorageService(BUCKET, null);

        String key = service.resolveObjectKeyWithFallback(
                "surfboards/media/image/stored-key",
                "https://s3.fr-par.scw.cloud/test-bucket/surfboards/media/image/from-url",
                "media-123",
                "image",
                "surfboards/media");

        assertEquals("surfboards/media/image/stored-key", key);
    }

    @Test
    void resolveObjectKeyWithFallback_returnsParsedUrlKey_whenStoredKeyMissing() {
        StorageService service = new StorageService(BUCKET, null);

        String key = service.resolveObjectKeyWithFallback(
                null,
                "https://s3.fr-par.scw.cloud/test-bucket/trips/media/video/from-url",
                "media-123",
                "video",
                "trips/media");

        assertEquals("trips/media/video/from-url", key);
    }

    @Test
    void resolveObjectKeyWithFallback_returnsGeneratedKey_whenStoredAndUrlUnavailable() {
        StorageService service = new StorageService(BUCKET, null);

        String key = service.resolveObjectKeyWithFallback(
                null,
                null,
                "media-123",
                "image",
                "surf-sessions/media");

        assertEquals("surf-sessions/media/image/media-123", key);
    }

    @Test
    void deleteObject_returnsTrue_whenS3ClientConfigured() {
        StorageService service = new StorageService(BUCKET, presigner, s3Client);
        when(s3Client.deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class)))
                .thenReturn(software.amazon.awssdk.services.s3.model.DeleteObjectResponse.builder().build());

        boolean deleted = service.deleteObject("trips/media/image/abc-123");

        assertTrue(deleted);
        verify(s3Client).deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
    }
}
