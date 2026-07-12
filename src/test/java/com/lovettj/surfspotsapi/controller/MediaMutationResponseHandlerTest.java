package com.lovettj.surfspotsapi.controller;

import com.lovettj.surfspotsapi.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaMutationResponseHandlerTest {

    @Test
    void addMediaCreatedShouldReturnCreatedWhenOperationSucceeds() {
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.addMediaCreated(
                () -> "m1",
                id -> URI.create("http://example.com/" + id)
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("m1", response.getBody().getData());
        assertEquals("Media added successfully", response.getBody().getMessage());
    }

    @Test
    void addMediaCreatedShouldPropagateResponseStatusException() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                MediaMutationResponseHandler.addMediaCreated(
                        () -> {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "gone");
                        },
                        id -> URI.create("http://example.com/" + id)
                ));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("gone", exception.getReason());
    }

    @Test
    void addMediaCreatedShouldPropagateUnexpectedException() {
        RuntimeException cause = new RuntimeException("boom");
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                MediaMutationResponseHandler.addMediaCreated(
                        () -> {
                            throw cause;
                        },
                        id -> URI.create("http://example.com/" + id)
                ));
        assertEquals(cause, thrown);
    }

    @Test
    void recordMediaOkShouldReturnOkWhenOperationSucceeds() {
        boolean[] called = {false};
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.recordMediaOk(
                () -> {
                    called[0] = true;
                }
        );
        assertTrue(called[0]);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Media recorded successfully", response.getBody().getData());
    }

    @Test
    void recordMediaOkShouldPropagateResponseStatusException() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                MediaMutationResponseHandler.recordMediaOk(
                        () -> {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "dup");
                        }
                ));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("dup", exception.getReason());
    }

    @Test
    void recordMediaOkShouldPropagateUnexpectedException() {
        RuntimeException cause = new RuntimeException("fail");
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                MediaMutationResponseHandler.recordMediaOk(
                        () -> {
                            throw cause;
                        }
                ));
        assertEquals(cause, thrown);
    }

    @Test
    void deleteMediaShouldReturnOkWhenOperationSucceeds() {
        boolean[] called = {false};
        ResponseEntity<ApiResponse<String>> response = MediaMutationResponseHandler.deleteMedia(
                () -> {
                    called[0] = true;
                }
        );
        assertTrue(called[0]);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Media deleted successfully", response.getBody().getData());
    }

    @Test
    void deleteMediaShouldPropagateResponseStatusException() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                MediaMutationResponseHandler.deleteMedia(
                        () -> {
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "nope");
                        }
                ));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("nope", exception.getReason());
    }

    @Test
    void deleteMediaShouldPropagateUnexpectedException() {
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                MediaMutationResponseHandler.deleteMedia(
                        () -> {
                            throw new RuntimeException("x");
                        }
                ));
        assertEquals("x", thrown.getMessage());
    }
}
