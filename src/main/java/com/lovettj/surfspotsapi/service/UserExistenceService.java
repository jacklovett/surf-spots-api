package com.lovettj.surfspotsapi.service;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.lovettj.surfspotsapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Caches user-existence checks so that the session filter does not issue a
 * database query on every authenticated request.
 *
 * Only positive results ("user exists") are cached. Negative results bypass
 * the cache so that a freshly registered user is recognised immediately.
 *
 * Each positive entry expires after TTL_MS. When a user is deleted, call
 * {@link #evict} to remove the entry immediately rather than waiting for the
 * TTL to elapse.
 */
@Service
@RequiredArgsConstructor
public class UserExistenceService {

    private static final long TTL_MS = 2 * 60 * 1_000L; // 2 minutes

    private final UserRepository userRepository;

    // Maps userId -> expiry timestamp (System.currentTimeMillis() + TTL_MS).
    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

    public boolean existsById(String userId) {
        Long expiry = cache.get(userId);
        if (expiry != null && expiry > System.currentTimeMillis()) {
            return true;
        }
        boolean exists = userRepository.existsById(userId);
        if (exists) {
            cache.put(userId, System.currentTimeMillis() + TTL_MS);
        } else {
            cache.remove(userId);
        }
        return exists;
    }

    /**
     * Removes the cached entry for the given user. Call this immediately after
     * deleting a user so that their session cookie is rejected on the next
     * request rather than after the TTL expires.
     */
    public void evict(String userId) {
        cache.remove(userId);
    }
}
