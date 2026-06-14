package com.marginalia.dev.services;

import com.marginalia.dev.dto.insights.InsightsResponse;

import java.util.UUID;

public interface InsightsService {

    InsightsResponse forUser(UUID userId);
}
