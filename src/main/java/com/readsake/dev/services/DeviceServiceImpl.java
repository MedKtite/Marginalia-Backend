package com.readsake.dev.services;

import com.readsake.dev.dto.device.DeviceRegisterRequest;
import com.readsake.dev.entities.Device;
import com.readsake.dev.exceptions.ForbiddenException;
import com.readsake.dev.exceptions.NotFoundException;
import com.readsake.dev.repo.dao.DeviceDao;
import com.readsake.dev.repo.dao.SubscriptionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeviceServiceImpl implements DeviceService {

    /** Pro tier cap. Free tier doesn't sync, so the cap doesn't apply meaningfully. */
    private static final int PRO_ACTIVE_DEVICE_CAP = 5;

    /** A device "counts" against the cap if it's been seen in the last N days. */
    private static final int ACTIVE_DAYS = 30;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private SubscriptionDao subscriptionDao;

    @Override
    @Transactional
    public Device register(DeviceRegisterRequest req, UUID userId) {
        Device d = new Device();
        d.setUserId(userId);
        d.setPlatform(req.getPlatform());
        d.setDeviceName(req.getDeviceName());
        d.setPushToken(req.getPushToken());
        d.setPushPermission(req.getPushPermission());

        // Pro 5-device cap. We only enforce on NEW devices (no matching push_token).
        boolean isExistingToken = req.getPushToken() != null
                && deviceDao.findByPushToken(req.getPushToken()).isPresent();
        if (!isExistingToken && subscriptionDao.isPro(userId)) {
            int active = deviceDao.countActiveByUser(userId, ACTIVE_DAYS);
            if (active >= PRO_ACTIVE_DEVICE_CAP) {
                throw new ForbiddenException(
                        "Device limit reached (" + PRO_ACTIVE_DEVICE_CAP +
                        " active in the last " + ACTIVE_DAYS + " days). " +
                        "Remove a device in Settings to add this one.");
            }
        }

        return deviceDao.upsertByPushToken(d);
    }

    @Override
    public List<Device> list(UUID userId) {
        return deviceDao.findByUser(userId);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID userId) {
        if (!deviceDao.delete(id, userId)) {
            throw new NotFoundException("Device not found");
        }
    }
}
