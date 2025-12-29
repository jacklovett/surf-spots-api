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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SurfboardService {

    private final SurfboardRepository surfboardRepository;
    private final SurfboardMediaRepository surfboardMediaRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public SurfboardService(
            SurfboardRepository surfboardRepository,
            SurfboardMediaRepository surfboardMediaRepository,
            UserRepository userRepository,
            StorageService storageService) {
        this.surfboardRepository = surfboardRepository;
        this.surfboardMediaRepository = surfboardMediaRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional
    public SurfboardDTO createSurfboard(String userId, CreateSurfboardRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Surfboard surfboard = Surfboard.builder()
                .user(user)
                .name(request.getName())
                .boardType(request.getBoardType())
                .length(request.getLength())
                .width(request.getWidth())
                .thickness(request.getThickness())
                .volume(request.getVolume())
                .finSetup(request.getFinSetup())
                .description(request.getDescription())
                .modelUrl(request.getModelUrl())
                .build();

        surfboard = surfboardRepository.save(surfboard);
        return new SurfboardDTO(surfboard);
    }

    @Transactional
    public SurfboardDTO updateSurfboard(String userId, String surfboardId, UpdateSurfboardRequest request) {
        Surfboard surfboard = surfboardRepository.findByIdAndUserId(surfboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surfboard not found"));

        if (request.getName() != null) {
            surfboard.setName(request.getName());
        }
        if (request.getBoardType() != null) {
            surfboard.setBoardType(request.getBoardType());
        }
        if (request.getLength() != null) {
            surfboard.setLength(request.getLength());
        }
        if (request.getWidth() != null) {
            surfboard.setWidth(request.getWidth());
        }
        if (request.getThickness() != null) {
            surfboard.setThickness(request.getThickness());
        }
        if (request.getVolume() != null) {
            surfboard.setVolume(request.getVolume());
        }
        if (request.getFinSetup() != null) {
            surfboard.setFinSetup(request.getFinSetup());
        }
        if (request.getDescription() != null) {
            surfboard.setDescription(request.getDescription());
        }
        if (request.getModelUrl() != null) {
            surfboard.setModelUrl(request.getModelUrl());
        }

        surfboard = surfboardRepository.save(surfboard);
        return new SurfboardDTO(surfboard);
    }

    @Transactional
    public void deleteSurfboard(String userId, String surfboardId) {
        Surfboard surfboard = surfboardRepository.findByIdAndUserId(surfboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surfboard not found"));

        // Delete all media first
        List<SurfboardMedia> media = surfboardMediaRepository.findBySurfboardId(surfboardId);
        surfboardMediaRepository.deleteAll(media);

        surfboardRepository.delete(surfboard);
    }

    public SurfboardDTO getSurfboard(String userId, String surfboardId) {
        Surfboard surfboard = surfboardRepository.findByIdAndUserId(surfboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surfboard not found"));

        return new SurfboardDTO(surfboard);
    }

    public List<SurfboardDTO> getUserSurfboards(String userId) {
        List<Surfboard> surfboards = surfboardRepository.findByUserId(userId);
        return surfboards.stream()
                .map(SurfboardDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public SurfboardMediaDTO addMedia(String userId, String surfboardId, CreateSurfboardMediaRequest request) {
        Surfboard surfboard = surfboardRepository.findByIdAndUserId(surfboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surfboard not found"));

        SurfboardMedia media = SurfboardMedia.builder()
                .surfboard(surfboard)
                .originalUrl(request.getOriginalUrl())
                .thumbUrl(request.getThumbUrl())
                .mediaType(request.getMediaType() != null ? request.getMediaType() : "image")
                .build();

        media = surfboardMediaRepository.save(media);
        return new SurfboardMediaDTO(media);
    }

    public String getUploadUrl(String userId, String surfboardId, String mediaType, String mediaId) {
        Surfboard surfboard = surfboardRepository.findByIdAndUserId(surfboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surfboard not found"));

        // Validate media type - must be 'image' or 'video'
        if (mediaType == null || (!mediaType.equals("image") && !mediaType.equals("video"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media type must be 'image' or 'video'");
        }

        // Determine content type based on media type
        String contentType = "image".equals(mediaType) ? "image/jpeg" : "video/mp4";
        
        // Generate S3 key for the media file
        String s3Key = storageService.generateMediaKey(mediaId, mediaType, "surfboards/media");
        
        // Generate presigned URL for uploading to S3
        return storageService.generatePresignedUploadUrl(s3Key, contentType);
    }

    @Transactional
    public void deleteMedia(String userId, String mediaId) {
        SurfboardMedia media = surfboardMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));

        // Verify the media belongs to a surfboard owned by the user
        if (!media.getSurfboard().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to delete this media");
        }

        surfboardMediaRepository.delete(media);
    }
}
