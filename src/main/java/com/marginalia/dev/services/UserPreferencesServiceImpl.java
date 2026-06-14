package com.marginalia.dev.services;

import com.marginalia.dev.dto.preferences.UserPreferencesUpdateRequest;
import com.marginalia.dev.entities.UserPreferences;
import com.marginalia.dev.repo.dao.UserPreferencesDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserPreferencesServiceImpl implements UserPreferencesService {

    private static final String DEFAULT_HOME_SECTIONS =
            "{\"passage_of_day\":true,\"continue_listening\":true,\"also_in_progress\":true}";

    @Autowired
    private UserPreferencesDao prefsDao;

    @Override
    public UserPreferences get(UUID userId) {
        return prefsDao.findByUserId(userId).orElseGet(() -> defaultsFor(userId));
    }

    @Override
    public UserPreferences update(UUID userId, UserPreferencesUpdateRequest req) {
        UserPreferences p = new UserPreferences();
        p.setUserId(userId);
        p.setTheme(req.getTheme());
        p.setDensity(req.getDensity());
        p.setGreetingOverride(req.getGreetingOverride());
        p.setHomeSections(req.getHomeSections());
        p.setReadingFontSize(req.getReadingFontSize());
        p.setReadingLineHeight(req.getReadingLineHeight());
        return prefsDao.upsert(p);
    }

    private UserPreferences defaultsFor(UUID userId) {
        UserPreferences p = new UserPreferences();
        p.setUserId(userId);
        p.setTheme("system");
        p.setDensity("spacious");
        p.setHomeSections(DEFAULT_HOME_SECTIONS);
        p.setReadingFontSize(17);
        p.setReadingLineHeight(165);
        return p;
    }
}
