package com.readsake.dev.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Discriminated union — one hit type per row. The mobile client reads the
 * {@code type} field and renders the appropriate cell shape.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchHit {

    public enum Type { book, highlight, note }

    private Type type;
    private UUID id;
    private UUID bookId;       // null on type=book (it IS the book)
    private UUID highlightId;  // only on type=note when the note is attached
    private String bookTitle;
    private String bookAuthor;

    private String title;      // book title (type=book)
    private String chapterRef; // §47, Chapter 4, ...  (type=highlight)
    private String colorTag;   // (type=highlight)
    private String snippet;    // <mark>matched</mark> text (type=highlight / note)
    private Double rank;       // ts_rank score (highlight / note) — null for books

    private String coverUrl;
    private String coverDominantColor;
    private String format;
    private String status;

    public static SearchHit book(UUID id, String title, String author, String coverUrl,
                                 String coverDominantColor, String format, String status) {
        SearchHit h = new SearchHit();
        h.type = Type.book;
        h.id = id;
        h.title = title;
        h.bookAuthor = author;
        h.coverUrl = coverUrl;
        h.coverDominantColor = coverDominantColor;
        h.format = format;
        h.status = status;
        return h;
    }

    public static SearchHit highlight(UUID id, UUID bookId, String bookTitle, String bookAuthor,
                                       String chapterRef, String colorTag, String snippet, Double rank) {
        SearchHit h = new SearchHit();
        h.type = Type.highlight;
        h.id = id;
        h.bookId = bookId;
        h.bookTitle = bookTitle;
        h.bookAuthor = bookAuthor;
        h.chapterRef = chapterRef;
        h.colorTag = colorTag;
        h.snippet = snippet;
        h.rank = rank;
        return h;
    }

    public static SearchHit note(UUID id, UUID bookId, UUID highlightId, String bookTitle,
                                  String snippet, Double rank) {
        SearchHit h = new SearchHit();
        h.type = Type.note;
        h.id = id;
        h.bookId = bookId;
        h.highlightId = highlightId;
        h.bookTitle = bookTitle;
        h.snippet = snippet;
        h.rank = rank;
        return h;
    }

    public Type getType() { return type; }
    public UUID getId() { return id; }
    public UUID getBookId() { return bookId; }
    public UUID getHighlightId() { return highlightId; }
    public String getBookTitle() { return bookTitle; }
    public String getBookAuthor() { return bookAuthor; }
    public String getTitle() { return title; }
    public String getChapterRef() { return chapterRef; }
    public String getColorTag() { return colorTag; }
    public String getSnippet() { return snippet; }
    public Double getRank() { return rank; }
    public String getCoverUrl() { return coverUrl; }
    public String getCoverDominantColor() { return coverDominantColor; }
    public String getFormat() { return format; }
    public String getStatus() { return status; }
}
