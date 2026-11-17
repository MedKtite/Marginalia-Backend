package com.readsake.dev.services;

import com.readsake.dev.repo.dao.GutenbergCatalogDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps the gutenberg_catalog table in sync with Project Gutenberg's published
 * catalog CSV (pg_catalog.csv, ~75k rows). Runs nightly, plus once at startup
 * when the table is empty so a fresh database self-populates. Metadata only —
 * EPUBs are still downloaded on-device at read time (see the Flutter
 * gutenberg_provider).
 */
@Service
public class GutenbergSyncService {

    private static final Logger log = LoggerFactory.getLogger(GutenbergSyncService.class);
    private static final int BATCH_SIZE = 1000;

    // pg_catalog.csv columns, in order.
    private static final int COL_ID = 0, COL_TYPE = 1, COL_TITLE = 3,
            COL_LANGUAGE = 4, COL_AUTHORS = 5, COL_SUBJECTS = 6;

    @Value("${gutenberg.catalog-url:https://gutenberg.pglaf.org/cache/epub/feeds/pg_catalog.csv}")
    private String catalogUrl;

    @Autowired
    private GutenbergCatalogDao dao;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Fresh DB → populate in the background without blocking startup. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            if (dao.count() > 0) return;
        } catch (Exception e) {
            log.warn("gutenberg_catalog not reachable at startup — is the table created?", e);
            return;
        }
        log.info("gutenberg_catalog is empty — starting initial sync in the background");
        CompletableFuture.runAsync(this::sync);
    }

    /** Nightly refresh — new Gutenberg releases land daily. */
    @Scheduled(cron = "0 30 4 * * *")
    public void nightly() {
        sync();
    }

    public void sync() {
        if (!running.compareAndSet(false, true)) {
            log.info("Gutenberg sync already running — skipping");
            return;
        }
        try {
            final HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(catalogUrl))
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();
            final HttpResponse<java.io.InputStream> resp =
                    http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != 200) {
                log.warn("Gutenberg catalog download failed: HTTP {}", resp.statusCode());
                return;
            }

            int total = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                List<String> record = readRecord(reader); // header
                final List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
                while ((record = readRecord(reader)) != null) {
                    final Object[] row = toRow(record);
                    if (row == null) continue;
                    batch.add(row);
                    if (batch.size() >= BATCH_SIZE) {
                        dao.upsertBatch(batch);
                        total += batch.size();
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    dao.upsertBatch(batch);
                    total += batch.size();
                }
            }
            log.info("Gutenberg catalog sync complete — {} titles upserted", total);
        } catch (Exception e) {
            log.warn("Gutenberg catalog sync failed", e);
        } finally {
            running.set(false);
        }
    }

    /** Text entries with a usable id + title become upsert rows; the rest null. */
    private static Object[] toRow(List<String> record) {
        if (record.size() <= COL_SUBJECTS) return null;
        if (!"Text".equals(record.get(COL_TYPE))) return null;
        final int id;
        try {
            id = Integer.parseInt(record.get(COL_ID).trim());
        } catch (NumberFormatException e) {
            return null;
        }
        // Multi-line titles in the CSV — flatten for storage/display.
        final String title = record.get(COL_TITLE).replaceAll("\\s+", " ").trim();
        if (title.isEmpty()) return null;
        final String authors = blankToNull(record.get(COL_AUTHORS));
        final String language = blankToNull(record.get(COL_LANGUAGE));
        final String subjects = blankToNull(record.get(COL_SUBJECTS));
        return new Object[]{id, title, authors, language, subjects, TextNormalizer.normalize(title)};
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /**
     * Reads one RFC-4180 CSV record (quoted fields may contain commas, quotes
     * doubled as "" and embedded newlines). Returns null at end of stream.
     */
    static List<String> readRecord(Reader reader) throws IOException {
        final List<String> fields = new ArrayList<>();
        final StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean sawAny = false;

        int c;
        while ((c = reader.read()) != -1) {
            sawAny = true;
            final char ch = (char) c;
            if (inQuotes) {
                if (ch == '"') {
                    reader.mark(1);
                    final int next = reader.read();
                    if (next == '"') {
                        field.append('"'); // escaped quote
                    } else {
                        inQuotes = false;
                        if (next != -1) reader.reset();
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n') {
                break;
            } else if (ch != '\r') {
                field.append(ch);
            }
        }
        if (!sawAny) return null;
        fields.add(field.toString());
        return fields;
    }
}
