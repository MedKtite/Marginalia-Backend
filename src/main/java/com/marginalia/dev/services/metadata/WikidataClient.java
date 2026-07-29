package com.marginalia.dev.services.metadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component
public class WikidataClient extends AbstractMetadataClient {
    public WikidataClient(ObjectMapper mapper) { super(mapper); }
    @Override public Optional<MetadataResult> find(String isbn, String title, String author) { try {
        String q = (title == null ? "" : title) + " " + (author == null ? "" : author);
        JsonNode hits = getJson("https://www.wikidata.org/w/api.php?action=wbsearchentities&search=" + encode(q) + "&language=en&format=json&limit=1").path("search");
        if (!hits.isArray() || hits.isEmpty()) return Optional.empty(); JsonNode hit = hits.path(0); String label = text(hit, "label");
        return label == null ? Optional.empty() : Optional.of(new MetadataResult("wikidata", isbn, label, java.util.List.of(), null, null, null, text(hit, "description"), null, hit.toString()));
    } catch (Exception e) { log.warn("Wikidata metadata lookup failed: {}", e.getMessage()); } return Optional.empty(); }
}
