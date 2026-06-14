package com.marginalia.dev.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {

    private String query;
    private String scope;            // "all" | "books" | "highlights" | "notes"
    private int totalHits;
    private Counts counts;
    private List<SearchHit> results;

    public static SearchResponse of(String query, String scope, List<SearchHit> results,
                                    int books, int highlights, int notes) {
        SearchResponse r = new SearchResponse();
        r.query     = query;
        r.scope     = scope;
        r.results   = results;
        r.totalHits = results.size();
        r.counts    = new Counts(books, highlights, notes);
        return r;
    }

    public String getQuery() { return query; }
    public String getScope() { return scope; }
    public int getTotalHits() { return totalHits; }
    public Counts getCounts() { return counts; }
    public List<SearchHit> getResults() { return results; }

    public static class Counts {
        private final int books;
        private final int highlights;
        private final int notes;

        public Counts(int books, int highlights, int notes) {
            this.books = books;
            this.highlights = highlights;
            this.notes = notes;
        }

        public int getBooks() { return books; }
        public int getHighlights() { return highlights; }
        public int getNotes() { return notes; }
    }
}
