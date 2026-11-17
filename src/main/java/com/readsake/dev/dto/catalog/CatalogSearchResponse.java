package com.readsake.dev.dto.catalog;

import java.util.List;

/**
 * Envelope for GET /me/catalog/search. [partialResults] is true when Google
 * Books was unreachable and [items] carries only the local Gutenberg layer —
 * the client shows what it has instead of an error.
 */
public class CatalogSearchResponse {

    private String query;
    private boolean partialResults;
    private String message;
    private List<CatalogVolumeResponse> items = List.of();

    public static CatalogSearchResponse of(String query, boolean partialResults,
                                           List<CatalogVolumeResponse> items) {
        final CatalogSearchResponse r = new CatalogSearchResponse();
        r.query = query;
        r.partialResults = partialResults;
        r.items = items;
        return r;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public boolean isPartialResults() { return partialResults; }
    public void setPartialResults(boolean partialResults) { this.partialResults = partialResults; }

    public List<CatalogVolumeResponse> getItems() { return items; }
    public void setItems(List<CatalogVolumeResponse> items) { this.items = items; }
}
