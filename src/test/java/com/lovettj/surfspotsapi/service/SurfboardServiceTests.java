package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.SurfboardDTO;
import com.lovettj.surfspotsapi.dto.SurfboardMediaDTO;
import com.lovettj.surfspotsapi.entity.Surfboard;
import com.lovettj.surfspotsapi.entity.SurfboardMedia;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.SurfboardMediaRepository;
import com.lovettj.surfspotsapi.repository.SurfboardRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.CreateSurfboardMediaRequest;
import com.lovettj.surfspotsapi.requests.CreateSurfboardRequest;
import com.lovettj.surfspotsapi.requests.UpdateSurfboardRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurfboardServiceTests {

    @Mock
    private SurfboardRepository surfboardRepository;

    @Mock
    private SurfboardMediaRepository surfboardMediaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private SurfboardService surfboardService;

    private User testUser;
    private Surfboard testSurfboard;
    private String userId;
    private String surfboardId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        surfboardId = UUID.randomUUID().toString();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .build();

        testSurfboard = Surfboard.builder()
                .id(surfboardId)
                .user(testUser)
                .name("Test Board")
                .boardType("shortboard")
                .length(BigDecimal.valueOf(6))
                .width(BigDecimal.valueOf(19.5))
                .thickness(BigDecimal.valueOf(2.5))
                .volume(BigDecimal.valueOf(30.5))
                .finSetup("thruster")
                .description("Test description")
                .build();
    }

    @Test
    void createSurfboardSuccess() {
        // Given
        CreateSurfboardRequest request = new CreateSurfboardRequest();
        request.setName("New Board");
        request.setBoardType("longboard");
        request.setLength(BigDecimal.valueOf(9));
        request.setWidth(BigDecimal.valueOf(23.0));
        request.setThickness(BigDecimal.valueOf(3.0));
        request.setVolume(BigDecimal.valueOf(70.0));
        request.setFinSetup("single");
        request.setDescription("A great longboard");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(surfboardRepository.save(any(Surfboard.class))).thenReturn(testSurfboard);

        // When
        SurfboardDTO result = surfboardService.createSurfboard(userId, request);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(surfboardRepository).save(any(Surfboard.class));
    }

    @Test
    void createSurfboardFailedUserNotFound() {
        // Given
        CreateSurfboardRequest request = new CreateSurfboardRequest();
        request.setName("New Board");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.createSurfboard(userId, request);
        });
    }

    @Test
    void updateSurfboardSuccess() {
        // Given
        UpdateSurfboardRequest request = new UpdateSurfboardRequest();
        request.setName("Updated Board");
        request.setBoardType("fish");

        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));
        when(surfboardRepository.save(any(Surfboard.class))).thenReturn(testSurfboard);

        // When
        SurfboardDTO result = surfboardService.updateSurfboard(userId, surfboardId, request);

        // Then
        assertNotNull(result);
        verify(surfboardRepository).findByIdAndUserId(surfboardId, userId);
        verify(surfboardRepository).save(any(Surfboard.class));
    }

    @Test
    void updateSurfboardFailedSurfboardNotFound() {
        // Given
        UpdateSurfboardRequest request = new UpdateSurfboardRequest();

        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.updateSurfboard(userId, surfboardId, request);
        });
    }

    @Test
    void deleteSurfboardSuccess() {
        // Given
        List<SurfboardMedia> media = Arrays.asList();
        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));
        when(surfboardMediaRepository.findBySurfboardId(surfboardId)).thenReturn(media);
        doNothing().when(surfboardRepository).delete(testSurfboard);

        // When
        surfboardService.deleteSurfboard(userId, surfboardId);

        // Then
        verify(surfboardRepository).findByIdAndUserId(surfboardId, userId);
        verify(surfboardMediaRepository).findBySurfboardId(surfboardId);
        verify(surfboardRepository).delete(testSurfboard);
    }

    @Test
    void deleteSurfboardFailedSurfboardNotFound() {
        // Given
        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.deleteSurfboard(userId, surfboardId);
        });
    }

    @Test
    void getSurfboardSuccess() {
        // Given
        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));

        // When
        SurfboardDTO result = surfboardService.getSurfboard(userId, surfboardId);

        // Then
        assertNotNull(result);
        assertEquals(surfboardId, result.getId());
        verify(surfboardRepository).findByIdAndUserId(surfboardId, userId);
    }

    @Test
    void getSurfboardFailedSurfboardNotFound() {
        // Given
        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.getSurfboard(userId, surfboardId);
        });
    }

    @Test
    void getUserSurfboardsSuccess() {
        // Given
        List<Surfboard> surfboards = Arrays.asList(testSurfboard);
        when(surfboardRepository.findByUserId(userId)).thenReturn(surfboards);

        // When
        List<SurfboardDTO> result = surfboardService.getUserSurfboards(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(surfboardRepository).findByUserId(userId);
    }

    @Test
    void addMediaSuccess() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        CreateSurfboardMediaRequest request = new CreateSurfboardMediaRequest();
        request.setOriginalUrl("https://example.com/media.jpg");
        request.setThumbUrl("https://example.com/thumb.jpg");

        SurfboardMedia testMedia = SurfboardMedia.builder()
                .id(mediaId)
                .surfboard(testSurfboard)
                .originalUrl(request.getOriginalUrl())
                .thumbUrl(request.getThumbUrl())
                .build();

        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));
        when(surfboardMediaRepository.save(any(SurfboardMedia.class))).thenReturn(testMedia);

        // When
        SurfboardMediaDTO result = surfboardService.addMedia(userId, surfboardId, request);

        // Then
        assertNotNull(result);
        verify(surfboardRepository).findByIdAndUserId(surfboardId, userId);
        verify(surfboardMediaRepository).save(any(SurfboardMedia.class));
    }

    @Test
    void addMediaFailedSurfboardNotFound() {
        // Given
        CreateSurfboardMediaRequest request = new CreateSurfboardMediaRequest();
        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.addMedia(userId, surfboardId, request);
        });
    }

    @Test
    void deleteMediaSuccess() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        SurfboardMedia testMedia = SurfboardMedia.builder()
                .id(mediaId)
                .surfboard(testSurfboard)
                .originalUrl("https://example.com/media.jpg")
                .build();

        when(surfboardMediaRepository.findById(mediaId)).thenReturn(Optional.of(testMedia));
        doNothing().when(surfboardMediaRepository).delete(testMedia);

        // When
        surfboardService.deleteMedia(userId, mediaId);

        // Then
        verify(surfboardMediaRepository).findById(mediaId);
        verify(surfboardMediaRepository).delete(testMedia);
    }

    @Test
    void deleteMediaFailedMediaNotFound() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        when(surfboardMediaRepository.findById(mediaId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.deleteMedia(userId, mediaId);
        });
    }

    @Test
    void deleteMediaFailedNotOwner() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        String otherUserId = UUID.randomUUID().toString();
        User otherUser = User.builder().id(otherUserId).build();
        Surfboard otherSurfboard = Surfboard.builder()
                .id(UUID.randomUUID().toString())
                .user(otherUser)
                .name("Other Board")
                .build();
        SurfboardMedia testMedia = SurfboardMedia.builder()
                .id(mediaId)
                .surfboard(otherSurfboard)
                .originalUrl("https://example.com/media.jpg")
                .build();

        when(surfboardMediaRepository.findById(mediaId)).thenReturn(Optional.of(testMedia));

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.deleteMedia(userId, mediaId);
        });
    }

    @Test
    void getUploadUrl_Success() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        String mediaType = "image";
        String expectedS3Key = "surfboards/media/image/" + mediaId;
        String expectedUploadUrl = "https://example.com/upload-url";

        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));
        when(storageService.generateMediaKey(mediaId, mediaType, "surfboards/media")).thenReturn(expectedS3Key);
        when(storageService.generatePresignedUploadUrl(expectedS3Key, "image/jpeg")).thenReturn(expectedUploadUrl);

        // When
        String result = surfboardService.getUploadUrl(userId, surfboardId, mediaType, mediaId);

        // Then
        assertEquals(expectedUploadUrl, result);
        verify(surfboardRepository).findByIdAndUserId(surfboardId, userId);
        verify(storageService).generateMediaKey(mediaId, mediaType, "surfboards/media");
        verify(storageService).generatePresignedUploadUrl(expectedS3Key, "image/jpeg");
    }

    @Test
    void getUploadUrl_SuccessVideo() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        String mediaType = "video";
        String expectedS3Key = "surfboards/media/video/" + mediaId;
        String expectedUploadUrl = "https://example.com/upload-url";

        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));
        when(storageService.generateMediaKey(mediaId, mediaType, "surfboards/media")).thenReturn(expectedS3Key);
        when(storageService.generatePresignedUploadUrl(expectedS3Key, "video/mp4")).thenReturn(expectedUploadUrl);

        // When
        String result = surfboardService.getUploadUrl(userId, surfboardId, mediaType, mediaId);

        // Then
        assertEquals(expectedUploadUrl, result);
        verify(storageService).generateMediaKey(mediaId, mediaType, "surfboards/media");
        verify(storageService).generatePresignedUploadUrl(expectedS3Key, "video/mp4");
    }

    @Test
    void getUploadUrl_SurfboardNotFound() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        String mediaType = "image";

        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.getUploadUrl(userId, surfboardId, mediaType, mediaId);
        });
        verify(storageService, never()).generateMediaKey(any(), any(), any());
    }

    @Test
    void getUploadUrl_InvalidMediaType() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        String mediaType = "invalid";

        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            surfboardService.getUploadUrl(userId, surfboardId, mediaType, mediaId);
        });
        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Media type must be 'image' or 'video'"));
        verify(storageService, never()).generateMediaKey(any(), any(), any());
    }

    @Test
    void getUploadUrl_NullMediaType() {
        // Given
        String mediaId = UUID.randomUUID().toString();
        String mediaType = null;

        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            surfboardService.getUploadUrl(userId, surfboardId, mediaType, mediaId);
        });
        assertEquals(400, exception.getStatusCode().value());
        verify(storageService, never()).generateMediaKey(any(), any(), any());
    }
}



