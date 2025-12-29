package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.SurfSpotNoteDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SurfSpotNote;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.repository.SurfSpotNoteRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.SurfSpotNoteRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class SurfSpotNoteService {
    private final SurfSpotNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final SurfSpotRepository surfSpotRepository;

    public SurfSpotNoteService(
            SurfSpotNoteRepository noteRepository,
            UserRepository userRepository,
            SurfSpotRepository surfSpotRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.surfSpotRepository = surfSpotRepository;
    }

    public SurfSpotNoteDTO getNoteForUserAndSpot(String userId, Long surfSpotId) {
        Optional<SurfSpotNote> note = noteRepository.findByUserIdAndSurfSpotId(userId, surfSpotId);
        return note.map(SurfSpotNoteDTO::fromEntity).orElse(null);
    }

    public SurfSpotNoteDTO saveNote(SurfSpotNoteRequest request, Long surfSpotId) {
        // Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate surf spot exists
        SurfSpot surfSpot = surfSpotRepository.findById(surfSpotId)
                .orElseThrow(() -> new RuntimeException("Surf spot not found"));

        // Check if note already exists
        Optional<SurfSpotNote> existingNote = noteRepository.findByUserIdAndSurfSpotId(
                request.getUserId(), surfSpotId);

        SurfSpotNote note;
        if (existingNote.isPresent()) {
            // Update existing note
            note = existingNote.get();
            note.setNoteText(request.getNoteText() != null ? request.getNoteText() : "");
            note.setPreferredTide(request.getPreferredTide());
            note.setPreferredSwellDirection(request.getPreferredSwellDirection());
            note.setPreferredWind(request.getPreferredWind());
            note.setPreferredSwellRange(request.getPreferredSwellRange());
            note.setSkillRequirement(request.getSkillRequirement());
        } else {
            // Create new note
            note = SurfSpotNote.builder()
                    .user(user)
                    .surfSpot(surfSpot)
                    .noteText(request.getNoteText() != null ? request.getNoteText() : "")
                    .preferredTide(request.getPreferredTide())
                    .preferredSwellDirection(request.getPreferredSwellDirection())
                    .preferredWind(request.getPreferredWind())
                    .preferredSwellRange(request.getPreferredSwellRange())
                    .skillRequirement(request.getSkillRequirement())
                    .build();
        }

        note = noteRepository.save(note);
        return SurfSpotNoteDTO.fromEntity(note);
    }

}
