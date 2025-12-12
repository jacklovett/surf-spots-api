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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SurfboardService {

    private final SurfboardRepository surfboardRepository;
    private final SurfboardImageRepository surfboardImageRepository;
    private final UserRepository userRepository;

    public SurfboardService(
            SurfboardRepository surfboardRepository,
            SurfboardImageRepository surfboardImageRepository,
            UserRepository userRepository) {
        this.surfboardRepository = surfboardRepository;
        this.surfboardImageRepository = surfboardImageRepository;
        this.userRepository = userRepository;
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

        // Delete all images first
        List<SurfboardImage> images = surfboardImageRepository.findBySurfboardId(surfboardId);
        surfboardImageRepository.deleteAll(images);

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
    public SurfboardImageDTO addImage(String userId, String surfboardId, CreateSurfboardImageRequest request) {
        Surfboard surfboard = surfboardRepository.findByIdAndUserId(surfboardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Surfboard not found"));

        SurfboardImage image = SurfboardImage.builder()
                .surfboard(surfboard)
                .originalUrl(request.getOriginalUrl())
                .thumbUrl(request.getThumbUrl())
                .build();

        image = surfboardImageRepository.save(image);
        return new SurfboardImageDTO(image);
    }

    @Transactional
    public void deleteImage(String userId, String imageId) {
        SurfboardImage image = surfboardImageRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));

        // Verify the image belongs to a surfboard owned by the user
        if (!image.getSurfboard().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to delete this image");
        }

        surfboardImageRepository.delete(image);
    }
}



