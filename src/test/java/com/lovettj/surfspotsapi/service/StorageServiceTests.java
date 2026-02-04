package com.lovettj.surfspotsapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceTests {

    private static final String BUCKET = "test-bucket";

    @Mock
    private S3Presigner presigner;

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
}
