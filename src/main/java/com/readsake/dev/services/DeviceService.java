package com.readsake.dev.services;

import com.readsake.dev.dto.device.DeviceRegisterRequest;
import com.readsake.dev.entities.Device;

import java.util.List;
import java.util.UUID;

public interface DeviceService {

    /** Idempotent — re-registering an existing FCM token just refreshes the row. */
    Device register(DeviceRegisterRequest req, UUID userId);

    List<Device> list(UUID userId);

    void delete(UUID id, UUID userId);
}
