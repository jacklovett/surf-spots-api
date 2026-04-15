package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.function.Function;

public final class MediaMutationResponseHandler {

    /**
     * Runs an add-media service operation and returns the created DTO.
     */
    @FunctionalInterface
    public interface MediaOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Runs a media service operation that does not return a value.
     */
    @FunctionalInterface
    public interface MediaVoidOperation {
        void execute() throws Exception;
    }

    private MediaMutationResponseHandler() {
    }

    public static <T> ResponseEntity<ApiResponse<T>> addMediaCreated(
            Logger logger,
            String resourceLabel,
            Object resourceIdentifier,
            String userFacingResourceName,
            MediaOperation<T> mediaOperation,
            Function<T, URI> locationForMedia) {
        try {
            T media = mediaOperation.execute();
            URI location = locationForMedia.apply(media);
            return ResponseEntity.created(location)
                    .body(ApiResponse.success(media, "Media added successfully", HttpStatus.CREATED.value()));
        } catch (ResponseStatusException responseStatusException) {
            return ResponseEntity.status(responseStatusException.getStatusCode())
                    .body(ApiResponse.error(responseStatusException.getReason(), responseStatusException.getStatusCode().value()));
        } catch (Exception exception) {
            String exceptionDetail = buildExceptionDetail(exception);
            logger.warn("add-media failed {}={}: {}, returning 500", resourceLabel, resourceIdentifier, exceptionDetail, exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ApiErrors.formatErrorMessage("add", userFacingResourceName),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        }
    }

    /**
     * Handles "record media" mutations that return a simple success message.
     */
    public static ResponseEntity<ApiResponse<String>> recordMediaOk(
            Logger logger,
            String tripId,
            String userFacingResourceName,
            MediaVoidOperation mediaOperation) {
        try {
            mediaOperation.execute();
            return ResponseEntity.ok(ApiResponse.success("Media recorded successfully"));
        } catch (ResponseStatusException responseStatusException) {
            return ResponseEntity.status(responseStatusException.getStatusCode())
                    .body(ApiResponse.error(responseStatusException.getReason(), responseStatusException.getStatusCode().value()));
        } catch (Exception exception) {
            String exceptionDetail = buildExceptionDetail(exception);
            logger.warn("recordMedia failed tripId={}: {}, returning 500", tripId, exceptionDetail, exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ApiErrors.formatErrorMessage("save", userFacingResourceName),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        }
    }

    /**
     * Handles media delete operations while preserving existing API responses.
     */
    public static ResponseEntity<ApiResponse<String>> deleteMedia(
            String userFacingResourceName,
            MediaVoidOperation mediaOperation) {
        try {
            mediaOperation.execute();
            return ResponseEntity.ok(ApiResponse.success("Media deleted successfully"));
        } catch (ResponseStatusException responseStatusException) {
            return ResponseEntity.status(responseStatusException.getStatusCode())
                    .body(ApiResponse.error(responseStatusException.getReason(), responseStatusException.getStatusCode().value()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            ApiErrors.formatErrorMessage("delete", userFacingResourceName),
                            HttpStatus.INTERNAL_SERVER_ERROR.value()
                    ));
        }
    }

    private static String buildExceptionDetail(Exception exception) {
        if (exception.getCause() == null) {
            return exception.getMessage();
        }
        return exception.getMessage() + "; cause: " + exception.getCause().getMessage();
    }
}
