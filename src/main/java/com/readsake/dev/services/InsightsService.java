package com.readsake.dev.services;

import com.readsake.dev.dto.insights.InsightsResponse;

import java.util.UUID;

public interface InsightsService {

    InsightsResponse forUser(UUID userId);
}
