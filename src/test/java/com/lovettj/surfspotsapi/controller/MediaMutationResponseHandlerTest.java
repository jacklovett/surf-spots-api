package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MediaMutationResponseHandlerTest {

    @Mock
    private Logger logger;

    @Test
    void addMediaCreatedShouldReturnCreatedWhenOperationSucceeds() {
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.addMediaCreated(
                logger,
                "id",
                "v1",
                "test media",
                () -> "m1",
                id -> URI.create("http://example.com/" + id)
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("m1", response.getBody().getData());
        assertEquals("Media added successfully", response.getBody().getMessage());
        verifyNoInteractions(logger);
    }

    @Test
    void addMediaCreatedShouldMapResponseStatusException() {
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.addMediaCreated(
                logger,
                "id",
                "v1",
                "test media",
                () -> {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "gone");
                },
                id -> URI.create("http://example.com/" + id)
        );
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("gone", response.getBody().getMessage());
        verifyNoInteractions(logger);
    }

    @Test
    void addMediaCreatedShouldReturn500AndLogOnUnexpectedException() {
        RuntimeException cause = new RuntimeException("boom");
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.addMediaCreated(
                logger,
                "id",
                "v1",
                "test media",
                () -> {
                    throw cause;
                },
                id -> URI.create("http://example.com/" + id)
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                ApiErrors.formatErrorMessage("add", "test media"),
                response.getBody().getMessage()
        );
        verify(logger).warn(
                eq("add-media failed {}={}: {}, returning 500"),
                eq("id"),
                eq("v1"),
                anyString(),
                eq(cause)
        );
    }

    @Test
    void recordMediaOkShouldReturnOkWhenOperationSucceeds() {
        boolean[] called = {false};
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.recordMediaOk(
                logger,
                "t1",
                "trip media",
                () -> {
                    called[0] = true;
                }
        );
        assertTrue(called[0]);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Media recorded successfully", response.getBody().getData());
        verifyNoInteractions(logger);
    }

    @Test
    void recordMediaOkShouldMapResponseStatusException() {
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.recordMediaOk(
                logger,
                "t1",
                "trip media",
                () -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "dup");
                }
        );
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("dup", response.getBody().getMessage());
        verifyNoInteractions(logger);
    }

    @Test
    void recordMediaOkShouldReturn500AndLogOnUnexpectedException() {
        RuntimeException cause = new RuntimeException("fail");
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.recordMediaOk(
                logger,
                "t1",
                "trip media",
                () -> {
                    throw cause;
                }
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                ApiErrors.formatErrorMessage("save", "trip media"),
                response.getBody().getMessage()
        );
        verify(logger).warn(
                eq("recordMedia failed tripId={}: {}, returning 500"),
                eq("t1"),
                anyString(),
                eq(cause)
        );
    }

    @Test
    void deleteMediaShouldReturnOkWhenOperationSucceeds() {
        boolean[] called = {false};
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.deleteMedia(
                "trip media",
                () -> {
                    called[0] = true;
                }
        );
        assertTrue(called[0]);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Media deleted successfully", response.getBody().getData());
    }

    @Test
    void deleteMediaShouldMapResponseStatusException() {
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.deleteMedia(
                "trip media",
                () -> {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "nope");
                }
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("nope", response.getBody().getMessage());
    }

    @Test
    void deleteMediaShouldReturn500OnUnexpectedException() {
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.deleteMedia(
                "trip media",
                () -> {
                    throw new RuntimeException("x");
                }
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                ApiErrors.formatErrorMessage("delete", "trip media"),
                response.getBody().getMessage()
        );
    }
}
