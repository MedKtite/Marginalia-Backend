package com.readsake.dev.services;

import java.util.Map;
import java.util.UUID;

public interface ExportService {

    byte[] exportCsv(UUID userId, UUID bookId);

    byte[] exportPdf(UUID userId, UUID bookId);

    Map<String, Object> getExportSummary(UUID userId);
}
