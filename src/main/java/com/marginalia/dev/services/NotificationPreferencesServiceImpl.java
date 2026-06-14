package com.marginalia.dev.services;

import com.marginalia.dev.dto.notifications.NotificationPreferencesUpdateRequest;
import com.marginalia.dev.entities.NotificationPreferences;
import com.marginalia.dev.repo.dao.NotificationPreferencesDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationPreferencesServiceImpl implements NotificationPreferencesService {

    private static final String DEFAULT_CATEGORIES =
            "{\"ritual\":true,\"passage\":true,\"listening_resume\":true,\"sync\":false,\"milestone\":false}";

    @Autowired
    private NotificationPreferencesDao prefsDao;

    @Override
    public NotificationPreferences get(UUID userId) {
        return prefsDao.findByUserId(userId).orElseGet(() -> defaultsFor(userId));
    }

    @Override
    public NotificationPreferences update(UUID userId, NotificationPreferencesUpdateRequest req) {
        NotificationPreferences p = new NotificationPreferences();
        p.setUserId(userId);
        p.setEnabled(req.getEnabled());
        p.setCategories(req.getCategories());
        p.setRitualTimeLocal(req.getRitualTimeLocal());
        p.setPassageTimeLocal(req.getPassageTimeLocal());
        p.setQuietStartLocal(req.getQuietStartLocal());
        p.setQuietEndLocal(req.getQuietEndLocal());
        return prefsDao.upsert(p);
    }

    private NotificationPreferences defaultsFor(UUID userId) {
        NotificationPreferences p = new NotificationPreferences();
        p.setUserId(userId);
        p.setEnabled(true);
        p.setCategories(DEFAULT_CATEGORIES);
        p.setRitualTimeLocal(19);
        p.setPassageTimeLocal(8);
        p.setQuietStartLocal(22);
        p.setQuietEndLocal(8);
        return p;
    }
}
