package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class MediaUploadUrlResponseHandler {

    private static final Logger logger = LoggerFactory.getLogger(MediaUploadUrlResponseHandler.class);

    private MediaUploadUrlResponseHandler() {
    }

    public static ResponseEntity<ApiResponse<Map<String, String>>> buildUploadUrlResponse(
            String resourceLabel,
            String resourceIdentifier,
            Function<String, String> uploadUrlGenerator) {
        String mediaId = UUID.randomUUID().toString();
        try {
            String uploadUrl = uploadUrlGenerator.apply(mediaId);
            return ResponseEntity.ok(ApiResponse.success(Map.of("uploadUrl", uploadUrl, "mediaId", mediaId)));
        } catch (Exception exception) {
            if (exception instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            String detail = detailMessage(exception);
            logger.warn(
                    "upload-url failed {}={}: {}, returning 503 MEDIA_UPLOAD_UNAVAILABLE",
                    resourceLabel,
                    resourceIdentifier,
                    detail,
                    exception
            );
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, ApiErrors.MEDIA_UPLOAD_UNAVAILABLE, exception);
        }
    }

    private static String detailMessage(Exception exception) {
        if (exception.getCause() == null) {
            return exception.getMessage();
        }
        return exception.getMessage() + "; cause: " + exception.getCause().getMessage();
    }
}
