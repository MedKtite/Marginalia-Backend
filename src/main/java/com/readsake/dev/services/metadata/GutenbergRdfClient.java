package com.readsake.dev.services.metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Summary and download count straight from gutenberg.org's per-book RDF record
 * (the same data Gutendex mirrors, without its latency). Raw carries
 * {"download_count": n} so callers read it like a Gutendex payload.
 */
@Component
public class GutenbergRdfClient extends AbstractMetadataClient {
    private static final Pattern SUMMARY = Pattern.compile("<pgterms:marc520>(.*?)</pgterms:marc520>", Pattern.DOTALL);
    private static final Pattern DOWNLOADS = Pattern.compile("<pgterms:downloads[^>]*>(\\d+)</pgterms:downloads>");
    private static final Pattern TITLE = Pattern.compile("<dcterms:title>(.*?)</dcterms:title>", Pattern.DOTALL);
    public GutenbergRdfClient(ObjectMapper mapper) { super(mapper); }
    @Override public Optional<MetadataResult> find(String isbn, String title, String author) { return Optional.empty(); }
    public Optional<MetadataResult> findById(int gutenbergId) { try {
        HttpRequest request = HttpRequest.newBuilder()
                // Direct cache path: /ebooks/{id}.rdf redirects https→http, which HttpClient won't follow.
                // PGLAF mirror: same files, stays up when www.gutenberg.org returns 503s.
                .uri(URI.create("https://gutenberg.pglaf.org/cache/epub/" + gutenbergId + "/pg" + gutenbergId + ".rdf"))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "Readsake/1.0 (book metadata resolver)")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw new IllegalStateException("HTTP " + response.statusCode());
        String rdf = response.body();
        String summary = group(SUMMARY, rdf); String downloads = group(DOWNLOADS, rdf);
        String raw = downloads == null ? "{}" : "{\"download_count\":" + downloads + "}";
        return Optional.of(new MetadataResult("gutenberg", null, group(TITLE, rdf), List.of(), null, null, null, summary, null, raw));
    } catch (Exception e) { log.warn("Gutenberg RDF lookup failed for {}: {}", gutenbergId, e.getMessage()); } return Optional.empty(); }
    private static String group(Pattern p, String s) { Matcher m = p.matcher(s); return m.find() ? unescape(m.group(1).trim()) : null; }
    private static String unescape(String s) { return s.replace("&#13;", "").replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&"); }
}
