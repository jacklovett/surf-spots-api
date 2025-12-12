package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.SurfboardDTO;
import com.lovettj.surfspotsapi.dto.SurfboardImageDTO;
import com.lovettj.surfspotsapi.entity.Surfboard;
import com.lovettj.surfspotsapi.entity.SurfboardImage;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.SurfboardImageRepository;
import com.lovettj.surfspotsapi.repository.SurfboardRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.CreateSurfboardImageRequest;
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
    private SurfboardImageRepository surfboardImageRepository;

    @Mock
    private UserRepository userRepository;

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
        List<SurfboardImage> images = Arrays.asList();
        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));
        when(surfboardImageRepository.findBySurfboardId(surfboardId)).thenReturn(images);
        doNothing().when(surfboardRepository).delete(testSurfboard);

        // When
        surfboardService.deleteSurfboard(userId, surfboardId);

        // Then
        verify(surfboardRepository).findByIdAndUserId(surfboardId, userId);
        verify(surfboardImageRepository).findBySurfboardId(surfboardId);
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
    void addImageSuccess() {
        // Given
        String imageId = UUID.randomUUID().toString();
        CreateSurfboardImageRequest request = new CreateSurfboardImageRequest();
        request.setOriginalUrl("https://example.com/image.jpg");
        request.setThumbUrl("https://example.com/thumb.jpg");

        SurfboardImage testImage = SurfboardImage.builder()
                .id(imageId)
                .surfboard(testSurfboard)
                .originalUrl(request.getOriginalUrl())
                .thumbUrl(request.getThumbUrl())
                .build();

        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.of(testSurfboard));
        when(surfboardImageRepository.save(any(SurfboardImage.class))).thenReturn(testImage);

        // When
        SurfboardImageDTO result = surfboardService.addImage(userId, surfboardId, request);

        // Then
        assertNotNull(result);
        verify(surfboardRepository).findByIdAndUserId(surfboardId, userId);
        verify(surfboardImageRepository).save(any(SurfboardImage.class));
    }

    @Test
    void addImageFailedSurfboardNotFound() {
        // Given
        CreateSurfboardImageRequest request = new CreateSurfboardImageRequest();
        when(surfboardRepository.findByIdAndUserId(surfboardId, userId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.addImage(userId, surfboardId, request);
        });
    }

    @Test
    void deleteImageSuccess() {
        // Given
        String imageId = UUID.randomUUID().toString();
        SurfboardImage testImage = SurfboardImage.builder()
                .id(imageId)
                .surfboard(testSurfboard)
                .originalUrl("https://example.com/image.jpg")
                .build();

        when(surfboardImageRepository.findById(imageId)).thenReturn(Optional.of(testImage));
        doNothing().when(surfboardImageRepository).delete(testImage);

        // When
        surfboardService.deleteImage(userId, imageId);

        // Then
        verify(surfboardImageRepository).findById(imageId);
        verify(surfboardImageRepository).delete(testImage);
    }

    @Test
    void deleteImageFailedImageNotFound() {
        // Given
        String imageId = UUID.randomUUID().toString();
        when(surfboardImageRepository.findById(imageId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.deleteImage(userId, imageId);
        });
    }

    @Test
    void deleteImageFailedNotOwner() {
        // Given
        String imageId = UUID.randomUUID().toString();
        String otherUserId = UUID.randomUUID().toString();
        User otherUser = User.builder().id(otherUserId).build();
        Surfboard otherSurfboard = Surfboard.builder()
                .id(UUID.randomUUID().toString())
                .user(otherUser)
                .name("Other Board")
                .build();
        SurfboardImage testImage = SurfboardImage.builder()
                .id(imageId)
                .surfboard(otherSurfboard)
                .originalUrl("https://example.com/image.jpg")
                .build();

        when(surfboardImageRepository.findById(imageId)).thenReturn(Optional.of(testImage));

        // When/Then
        assertThrows(ResponseStatusException.class, () -> {
            surfboardService.deleteImage(userId, imageId);
        });
    }
}



