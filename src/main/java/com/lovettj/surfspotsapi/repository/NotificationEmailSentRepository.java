package com.lovettj.surfspotsapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lovettj.surfspotsapi.entity.NotificationEmailSent;

public interface NotificationEmailSentRepository
        extends JpaRepository<NotificationEmailSent, Long> {

    boolean existsByUserIdAndNotificationKey(String userId, String notificationKey);
}
