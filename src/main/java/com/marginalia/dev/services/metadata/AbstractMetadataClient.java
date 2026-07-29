package com.marginalia.dev.services.metadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
abstract class AbstractMetadataClient implements MetadataClient {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper mapper;
    protected final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    protected AbstractMetadataClient(ObjectMapper mapper) { this.mapper = mapper; }
    protected JsonNode getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/json")
                .header("User-Agent", "Marginalia/1.0 (book metadata resolver)")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + response.statusCode());
        return mapper.readTree(response.body());
    }
    protected static String encode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
    protected static String text(JsonNode node, String field) { JsonNode value = node.path(field); return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null; }
    protected static Integer integer(JsonNode node, String field) { JsonNode value = node.path(field); return value.isIntegralNumber() ? value.asInt() : null; }
    protected static List<String> textArray(JsonNode node, String field) { List<String> result = new ArrayList<>(); JsonNode values = node.path(field); if (values.isArray()) values.forEach(v -> { if (v.isTextual() && !v.asText().isBlank()) result.add(v.asText().trim()); }); return result; }
    protected static List<String> objectTextArray(JsonNode node, String field, String valueField) { List<String> result = new ArrayList<>(); JsonNode values = node.path(field); if (values.isArray()) values.forEach(v -> { String value = text(v, valueField); if (value != null) result.add(value); }); return result; }
    protected static String firstText(JsonNode node, String field) { JsonNode values = node.path(field); if (values.isTextual()) return values.asText(); if (values.isArray()) for (JsonNode value : values) if (value.isTextual() && !value.asText().isBlank()) return value.asText().trim(); return null; }
    protected static String isbn13(JsonNode values) { if (values.isArray()) for (JsonNode value : values) { String isbn = value.asText("").replace("-", "").trim(); if (isbn.length() == 13 && isbn.chars().allMatch(Character::isDigit)) return isbn; } return null; }
    protected static String firstIsbn(JsonNode node) { JsonNode ids = node.path("industryIdentifiers"); if (ids.isArray()) for (JsonNode id : ids) { String value = text(id, "identifier"); if (value != null && value.replace("-", "").length() == 13) return value.replace("-", ""); } return null; }
}
