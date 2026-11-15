package com.readsake.dev.services.metadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component
public class OpenLibraryClient extends AbstractMetadataClient {
    public OpenLibraryClient(ObjectMapper mapper) { super(mapper); }
    @Override public Optional<MetadataResult> find(String isbn, String title, String author) { try {
        String fields = "key,title,author_name,publisher,first_publish_year,number_of_pages_median,isbn,cover_i";
        String url = "https://openlibrary.org/search.json?title=" + encode(title) + "&author=" + encode(author) + "&limit=5&fields=" + encode(fields);
        JsonNode root = getJson(url); JsonNode docs = root.path("docs"); if (!docs.isArray() || docs.isEmpty()) return Optional.empty();
        JsonNode book = docs.path(0); String foundTitle = text(book, "title"); if (foundTitle == null) return Optional.empty();
        String foundIsbn = isbn != null ? isbn : isbn13(book.path("isbn")); Integer coverId = integer(book, "cover_i"); String cover = coverId == null ? null : "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
        String description = null; String workKey = text(book, "key"); if (workKey != null && workKey.startsWith("/works/")) { JsonNode value = getJson("https://openlibrary.org" + workKey + ".json").path("description"); description = value.isTextual() ? value.asText() : text(value, "value"); }
        return Optional.of(new MetadataResult("open_library", foundIsbn, foundTitle, textArray(book, "author_name"), firstText(book, "publisher"), book.path("first_publish_year").isIntegralNumber() ? book.path("first_publish_year").asInt() : null, integer(book, "number_of_pages_median"), description, cover, root.toString()));
    } catch (Exception e) { log.warn("Open Library metadata lookup failed: {}", e.getMessage()); } return Optional.empty(); }
}
