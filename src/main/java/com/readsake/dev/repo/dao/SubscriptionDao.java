package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.Subscription;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionDao {

    Optional<Subscription> findByUserId(UUID userId);

    /** True when the user has an active or in-grace-period Pro subscription. */
    boolean isPro(UUID userId);

    /** Insert or update (matched by user_id). */
    void upsert(Subscription sub);
}
