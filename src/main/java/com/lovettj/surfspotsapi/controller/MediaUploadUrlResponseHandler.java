package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

public final class MediaUploadUrlResponseHandler {

    @FunctionalInterface
    public interface UploadUrlGenerator {
        String generate(String mediaId) throws Exception;
    }

    private MediaUploadUrlResponseHandler() {
    }

    public static ResponseEntity<ApiResponse<Map<String, String>>> buildUploadUrlResponse(
            Logger logger,
            String resourceLabel,
            String resourceIdentifier,
            UploadUrlGenerator uploadUrlGenerator) {
        try {
            String mediaId = UUID.randomUUID().toString();
            String uploadUrl = uploadUrlGenerator.generate(mediaId);
            return ResponseEntity.ok(ApiResponse.success(Map.of("uploadUrl", uploadUrl, "mediaId", mediaId)));
        } catch (ResponseStatusException responseStatusException) {
            return ResponseEntity.status(responseStatusException.getStatusCode())
                    .body(ApiResponse.error(responseStatusException.getReason(), responseStatusException.getStatusCode().value()));
        } catch (Exception exception) {
            String detail = detailMessage(exception);
            logger.warn(
                    "upload-url failed {}={}: {}, returning 503 MEDIA_UPLOAD_UNAVAILABLE",
                    resourceLabel,
                    resourceIdentifier,
                    detail,
                    exception
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(ApiErrors.MEDIA_UPLOAD_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value()));
        }
    }

    private static String detailMessage(Exception exception) {
        if (exception.getCause() == null) {
            return exception.getMessage();
        }
        return exception.getMessage() + "; cause: " + exception.getCause().getMessage();
    }
}
