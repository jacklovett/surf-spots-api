package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MediaMutationResponseHandler {

    private MediaMutationResponseHandler() {
    }

    public static <T> ResponseEntity<ApiResponse<T>> addMediaCreated(
            Supplier<T> mediaOperation,
            Function<T, URI> locationForMedia) {
        T media = mediaOperation.get();
        URI location = locationForMedia.apply(media);
        return ResponseEntity.created(location)
                .body(ApiResponse.success(media, "Media added successfully", HttpStatus.CREATED.value()));
    }

    /**
     * Handles "record media" mutations that return a simple success message.
     */
    public static ResponseEntity<ApiResponse<String>> recordMediaOk(Runnable mediaOperation) {
        mediaOperation.run();
        return ResponseEntity.ok(ApiResponse.success("Media recorded successfully"));
    }

    /**
     * Handles media delete operations while preserving existing API responses.
     */
    public static ResponseEntity<ApiResponse<String>> deleteMedia(Runnable mediaOperation) {
        mediaOperation.run();
        return ResponseEntity.ok(ApiResponse.success("Media deleted successfully"));
    }
}
