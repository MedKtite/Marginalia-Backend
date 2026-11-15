package com.readsake.dev.services.metadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component
public class GutendexClient extends AbstractMetadataClient {
    public GutendexClient(ObjectMapper mapper) { super(mapper); }
    @Override public Optional<MetadataResult> find(String isbn, String title, String author) { try { JsonNode results = getJson("https://gutendex.com/books/?search=" + encode((title == null ? "" : title) + " " + (author == null ? "" : author))).path("results"); if (!results.isArray() || results.isEmpty()) return Optional.empty(); JsonNode book = results.path(0); String description = firstText(book, "summaries"); JsonNode formats = book.path("formats"); String cover = text(formats, "image/jpeg"); return Optional.of(new MetadataResult("gutendex", isbn, text(book, "title"), objectTextArray(book, "authors", "name"), null, null, null, description, cover, book.toString())); } catch (Exception e) { log.warn("Gutendex metadata lookup failed: {}", e.getMessage()); } return Optional.empty(); }
}
