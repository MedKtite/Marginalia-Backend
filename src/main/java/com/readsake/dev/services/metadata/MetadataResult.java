package com.readsake.dev.services.metadata;
import java.util.List;
public record MetadataResult(String source, String isbn13, String title, List<String> authors,
                             String publisher, Integer publishedYear, Integer pageCount,
                             String description, String coverUrl, String raw) {
    public boolean hasAnyValue() { return title != null || authors != null || publisher != null || publishedYear != null || pageCount != null || description != null || coverUrl != null || isbn13 != null; }
}
