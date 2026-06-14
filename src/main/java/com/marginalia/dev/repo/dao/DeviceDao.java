package com.marginalia.dev.repo.dao;

import com.marginalia.dev.entities.Device;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceDao {

    Optional<Device> findByIdAndUser(UUID id, UUID userId);

    Optional<Device> findByPushToken(String token);

    List<Device> findByUser(UUID userId);

    int countActiveByUser(UUID userId, int activeWithinDays);

    /**
     * Insert a new device OR update the existing row matched by push_token.
     * Returns the resulting row.
     */
    Device upsertByPushToken(Device device);

    Device insert(Device device);

    boolean delete(UUID id, UUID userId);
}
