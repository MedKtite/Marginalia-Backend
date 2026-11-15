package com.readsake.dev.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Ensures the two partitioned tables (audit_log, notification_log) always
 * have partitions ready for the current and next month.
 *
 * Without this, inserts fail at the start of every month with "no partition
 * of relation ... found for row".
 *
 * Runs on app startup AND once a day. Idempotent — uses CREATE TABLE IF NOT
 * EXISTS so re-running is harmless.
 */
@Component
public class PartitionMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(PartitionMaintenanceScheduler.class);

    private static final List<String> PARTITIONED_TABLES = List.of(
            "audit_log",
            "notification_log"
    );

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    private JdbcTemplate jdbc;

    /** Runs once when the app is fully started. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        ensureCurrentAndNextMonth();
    }

    /** 02:30 UTC every day. Gives us a 21+ hour head start before any midnight UTC user is affected. */
    @Scheduled(cron = "0 30 2 * * *")
    public void daily() {
        ensureCurrentAndNextMonth();
    }

    private void ensureCurrentAndNextMonth() {
        YearMonth current = YearMonth.now();
        YearMonth next    = current.plusMonths(1);
        for (String table : PARTITIONED_TABLES) {
            ensurePartition(table, current);
            ensurePartition(table, next);
        }
    }

    private void ensurePartition(String table, YearMonth ym) {
        String suffix         = String.format("%04d_%02d", ym.getYear(), ym.getMonthValue());
        String partitionName  = table + "_" + suffix;
        LocalDate start       = ym.atDay(1);
        LocalDate end         = ym.plusMonths(1).atDay(1);

        // Table/identifier names are interpolated from a hardcoded allowlist
        // (PARTITIONED_TABLES + computed yyyy_mm), so no SQL injection vector.
        String ddl = String.format(
                "CREATE TABLE IF NOT EXISTS %s PARTITION OF %s " +
                        "FOR VALUES FROM ('%s') TO ('%s')",
                partitionName, table, ISO_DATE.format(start), ISO_DATE.format(end));
        try {
            jdbc.execute((ConnectionCallback<Void>) connection -> {
                try (var statement = connection.createStatement()) {
                    statement.execute("SELECT pg_advisory_lock(731947205)");
                    try {
                        statement.execute(ddl);
                    } finally {
                        statement.execute("SELECT pg_advisory_unlock(731947205)");
                    }
                }
                return null;
            });
            log.debug("Ensured partition {}", partitionName);
        } catch (Exception e) {
            log.warn("Failed to ensure partition {}: {}", partitionName, e.getMessage());
        }
    }
}
