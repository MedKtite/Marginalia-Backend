package com.marginalia.dev.services;

import com.marginalia.dev.dto.search.SearchHit;
import com.marginalia.dev.dto.search.SearchResponse;
import com.marginalia.dev.repo.dao.SearchDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private SearchDao searchDao;

    @Override
    public SearchResponse search(UUID userId, String query, String scope, int limit) {
        if (query == null || query.isBlank()) {
            return SearchResponse.of(query, scope, Collections.emptyList(), 0, 0, 0);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        String s = scope == null ? "all" : scope.toLowerCase();

        List<SearchHit> books      = "all".equals(s) || "books".equals(s)
                ? mapBooks(searchDao.searchBooks(userId, query, safeLimit)) : List.of();
        List<SearchHit> highlights = "all".equals(s) || "highlights".equals(s)
                ? mapHighlights(searchDao.searchHighlights(userId, query, safeLimit)) : List.of();
        List<SearchHit> notes      = "all".equals(s) || "notes".equals(s)
                ? mapNotes(searchDao.searchNotes(userId, query, safeLimit)) : List.of();

        // Interleave: books, then highlights ranked, then notes ranked.
        List<SearchHit> combined = new ArrayList<>(books.size() + highlights.size() + notes.size());
        combined.addAll(books);
        combined.addAll(highlights);
        combined.addAll(notes);

        return SearchResponse.of(query, s, combined, books.size(), highlights.size(), notes.size());
    }

    private List<SearchHit> mapBooks(List<Map<String, Object>> rows) {
        List<SearchHit> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            out.add(SearchHit.book(
                    (UUID) row.get("id"),
                    (String) row.get("title"),
                    (String) row.get("author"),
                    (String) row.get("cover_url"),
                    (String) row.get("cover_dominant_color"),
                    (String) row.get("format"),
                    (String) row.get("status")));
        }
        return out;
    }

    private List<SearchHit> mapHighlights(List<Map<String, Object>> rows) {
        List<SearchHit> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object rank = row.get("rank");
            out.add(SearchHit.highlight(
                    (UUID) row.get("id"),
                    (UUID) row.get("book_id"),
                    (String) row.get("book_title"),
                    (String) row.get("book_author"),
                    (String) row.get("text_chapter_ref"),
                    (String) row.get("color_tag"),
                    (String) row.get("snippet"),
                    rank == null ? null : ((Number) rank).doubleValue()));
        }
        return out;
    }

    private List<SearchHit> mapNotes(List<Map<String, Object>> rows) {
        List<SearchHit> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object rank = row.get("rank");
            out.add(SearchHit.note(
                    (UUID) row.get("id"),
                    (UUID) row.get("book_id"),
                    (UUID) row.get("highlight_id"),
                    (String) row.get("book_title"),
                    (String) row.get("snippet"),
                    rank == null ? null : ((Number) rank).doubleValue()));
        }
        return out;
    }
}
