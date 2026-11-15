package com.readsake.dev.services.metadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component
public class GoogleBooksClient extends AbstractMetadataClient {
    @Value("${google.books.api-key:}") private String apiKey;
    public GoogleBooksClient(ObjectMapper mapper) { super(mapper); }
    @Override public Optional<MetadataResult> find(String isbn, String title, String author) { try {
        String query = isbn != null && !isbn.isBlank() ? "isbn:" + isbn : "intitle:" + title + (author == null || author.isBlank() ? "" : "+inauthor:" + author);
        String url = "https://www.googleapis.com/books/v1/volumes?q=" + encode(query) + "&maxResults=5&printType=books" + (apiKey == null || apiKey.isBlank() ? "" : "&key=" + encode(apiKey));
        JsonNode items = getJson(url).path("items"); if (!items.isArray()) return Optional.empty();
        for (JsonNode item : items) { JsonNode info = item.path("volumeInfo"); String foundTitle = text(info, "title"); if (foundTitle == null) continue; JsonNode links = info.path("imageLinks");
            return Optional.of(new MetadataResult("google_books", firstIsbn(info), foundTitle, textArray(info, "authors"), text(info, "publisher"), year(text(info, "publishedDate")), integer(info, "pageCount"), text(info, "description"), text(links, "thumbnail"), item.toString())); }
    } catch (Exception e) { log.warn("Google Books metadata lookup failed: {}", e.getMessage()); } return Optional.empty(); }
    private static Integer year(String date) { if (date == null || date.length() < 4) return null; try { return Integer.parseInt(date.substring(0, 4)); } catch (NumberFormatException e) { return null; } }
}
