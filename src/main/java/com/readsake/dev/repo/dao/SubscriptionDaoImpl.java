package com.readsake.dev.repo.dao;

import com.readsake.dev.entities.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class SubscriptionDaoImpl implements SubscriptionDao {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final BeanPropertyRowMapper<Subscription> MAPPER =
            new BeanPropertyRowMapper<>(Subscription.class);

    private static final String COLUMNS = """
            user_id, tier, status, store, revenuecat_id, product_id,
            current_period_start, current_period_end, cancel_at, trial_end, updated_at
            """;

    @Override
    public Optional<Subscription> findByUserId(UUID userId) {
        try {
            Subscription s = jdbc.queryForObject(
                    "SELECT " + COLUMNS + " FROM subscriptions WHERE user_id = :uid",
                    new MapSqlParameterSource("uid", userId),
                    MAPPER);
            return Optional.ofNullable(s);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean isPro(UUID userId) {
        return findByUserId(userId)
                .map(s -> "pro".equals(s.getTier())
                        && (s.getStatus() == null
                            || "active".equals(s.getStatus())
                            || "in_grace_period".equals(s.getStatus())
                            || "billing_retry".equals(s.getStatus())))
                .orElse(false);
    }

    @Override
    public void upsert(Subscription s) {
        String sql = """
                INSERT INTO subscriptions (
                    user_id, tier, status, store, revenuecat_id, product_id,
                    current_period_start, current_period_end, cancel_at, trial_end
                ) VALUES (
                    :userId, :tier, :status, :store, :revenuecatId, :productId,
                    :currentPeriodStart, :currentPeriodEnd, :cancelAt, :trialEnd
                )
                ON CONFLICT (user_id) DO UPDATE SET
                    tier                 = EXCLUDED.tier,
                    status               = EXCLUDED.status,
                    store                = EXCLUDED.store,
                    revenuecat_id        = EXCLUDED.revenuecat_id,
                    product_id           = EXCLUDED.product_id,
                    current_period_start = EXCLUDED.current_period_start,
                    current_period_end   = EXCLUDED.current_period_end,
                    cancel_at            = EXCLUDED.cancel_at,
                    trial_end            = EXCLUDED.trial_end,
                    updated_at           = NOW()
                """;

        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("userId",             s.getUserId())
                .addValue("tier",               s.getTier() != null ? s.getTier() : "free")
                .addValue("status",             s.getStatus())
                .addValue("store",              s.getStore())
                .addValue("revenuecatId",       s.getRevenuecatId())
                .addValue("productId",          s.getProductId())
                .addValue("currentPeriodStart", s.getCurrentPeriodStart())
                .addValue("currentPeriodEnd",   s.getCurrentPeriodEnd())
                .addValue("cancelAt",           s.getCancelAt())
                .addValue("trialEnd",           s.getTrialEnd()));
    }
}
