package com.lovettj.surfspotsapi.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.SessionStatus;
import com.lovettj.surfspotsapi.repository.SurfSessionRepository;
import com.lovettj.surfspotsapi.util.StringUtils;

@Service
public class LiveSessionOverdueNotificationService {

    private static final Logger logger =
            LoggerFactory.getLogger(LiveSessionOverdueNotificationService.class);

    private final SurfSessionRepository surfSessionRepository;
    private final SessionNotificationService sessionNotificationService;

    public LiveSessionOverdueNotificationService(
            SurfSessionRepository surfSessionRepository,
            SessionNotificationService sessionNotificationService) {
        this.surfSessionRepository = surfSessionRepository;
        this.sessionNotificationService = sessionNotificationService;
    }

    @Transactional
    public int processOverdueSessions() {
        Instant now = Instant.now();
        List<Long> overdueSessionIds =
                surfSessionRepository.findOverdueLiveSessionIdsNeedingNotification(
                        SessionStatus.IN_PROGRESS, now);

        int sentCount = 0;
        for (Long sessionId : overdueSessionIds) {
            if (processOverdueSessionById(sessionId, now)) {
                sentCount++;
            }
        }
        return sentCount;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processOverdueSessionById(Long sessionId, Instant now) {
        SurfSession session =
                surfSessionRepository.findByIdWithUserForUpdate(sessionId).orElse(null);
        if (session == null) {
            return false;
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS
                || session.getOverdueNotificationSentAt() != null
                || !session.isShareLocationWithEmergencyContact()
                || session.getExpectedReturnInstant() == null
                || !session.getExpectedReturnInstant().isBefore(now)) {
            return false;
        }

        return sendOverdueNotificationForSession(session, now);
    }

    boolean sendOverdueNotificationForSession(SurfSession session, Instant sentAt) {
        User user = session.getUser();
        String contactEmail = StringUtils.blankToNull(user.getEmergencyContactEmail());
        if (contactEmail == null) {
            logger.info(
                    "Marking overdue notification skipped for session {}: emergency contact email not set",
                    session.getId());
            session.setOverdueNotificationSentAt(sentAt);
            surfSessionRepository.save(session);
            return false;
        }

        try {
            sessionNotificationService.notifySessionOverdue(user, session);
            session.setOverdueNotificationSentAt(sentAt);
            surfSessionRepository.save(session);
            return true;
        } catch (RuntimeException sendException) {
            logger.warn(
                    "Overdue notification email failed for session {}: {}",
                    session.getId(),
                    sendException.getMessage(),
                    sendException);
            return false;
        }
    }
}
