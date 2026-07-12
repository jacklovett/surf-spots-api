package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.SurfSpotNoteRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.SurfSpotNoteRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;

@ExtendWith(MockitoExtension.class)
class SurfSpotNoteServiceTest {

    @Mock
    private SurfSpotNoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SurfSpotRepository surfSpotRepository;

    @InjectMocks
    private SurfSpotNoteService surfSpotNoteService;

    @Test
    void saveNoteShouldThrowNotFoundWhenUserMissing() {
        SurfSpotNoteRequest request = new SurfSpotNoteRequest();
        request.setUserId("missing-user");

        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> surfSpotNoteService.saveNote(request, 1L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(ApiErrors.USER_NOT_FOUND, exception.getReason());
    }

    @Test
    void saveNoteShouldThrowNotFoundWhenSurfSpotMissing() {
        SurfSpotNoteRequest request = new SurfSpotNoteRequest();
        request.setUserId("user-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(new User()));
        when(surfSpotRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> surfSpotNoteService.saveNote(request, 99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(ApiErrors.SURF_SPOT_NOT_FOUND, exception.getReason());
    }
}
