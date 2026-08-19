package com.marginalia.dev.services;

import com.marginalia.dev.entities.Book;
import com.marginalia.dev.entities.Highlight;
import com.marginalia.dev.entities.Note;
import com.marginalia.dev.repo.dao.BookDao;
import com.marginalia.dev.repo.dao.HighlightDao;
import com.marginalia.dev.repo.dao.NoteDao;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    @Autowired
    private BookDao bookDao;

    @Autowired
    private HighlightDao highlightDao;

    @Autowired
    private NoteDao noteDao;

    @Override
    public byte[] exportCsv(UUID userId, UUID bookId) {
        List<Book> books = bookId != null
                ? bookDao.findByIdAndUser(bookId, userId).map(List::of).orElse(List.of())
                : bookDao.findByUser(userId, null);

        Map<UUID, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, b -> b));

        List<Highlight> highlights = new ArrayList<>();
        if (bookId != null) {
            highlights.addAll(highlightDao.findByBook(bookId, userId));
        } else {
            for (Book b : books) {
                highlights.addAll(highlightDao.findByBook(b.getId(), userId));
            }
        }

        List<Note> allNotes = new ArrayList<>();
        if (bookId != null) {
            allNotes.addAll(noteDao.findByBook(bookId, userId));
        } else {
            for (Book b : books) {
                allNotes.addAll(noteDao.findByBook(b.getId(), userId));
            }
        }
        Map<UUID, List<Note>> notesByHighlight = allNotes.stream()
                .filter(n -> n.getHighlightId() != null)
                .collect(Collectors.groupingBy(Note::getHighlightId));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // CSV Header
        pw.println("Book Title,Author,Chapter / Reference,Highlight Passage,Color Tag,Attached Note,Saved,Created Date");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Highlight h : highlights) {
            Book b = bookMap.get(h.getBookId());
            String title = b != null ? b.getTitle() : "Unknown";
            String author = b != null && b.getAuthor() != null ? b.getAuthor() : "";
            String ref = h.getTextChapterRef() != null ? h.getTextChapterRef() : "";
            String passage = h.getPassageText() != null ? h.getPassageText() : "";
            String tag = h.getColorTag() != null ? h.getColorTag() : "";
            String saved = Boolean.TRUE.equals(h.getIsSaved()) ? "Yes" : "No";
            String date = h.getCreatedAt() != null ? h.getCreatedAt().format(dtf) : "";

            List<Note> attachedNotes = notesByHighlight.getOrDefault(h.getId(), List.of());
            String noteBody = attachedNotes.stream().map(Note::getBodyMd).collect(Collectors.joining("; "));

            pw.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                    escapeCsv(title),
                    escapeCsv(author),
                    escapeCsv(ref),
                    escapeCsv(passage),
                    escapeCsv(tag),
                    escapeCsv(noteBody),
                    escapeCsv(saved),
                    escapeCsv(date)));
        }

        pw.flush();
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportPdf(UUID userId, UUID bookId) {
        List<Book> books = bookId != null
                ? bookDao.findByIdAndUser(bookId, userId).map(List::of).orElse(List.of())
                : bookDao.findByUser(userId, null);

        Map<UUID, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, b -> b));

        List<Highlight> highlights = new ArrayList<>();
        if (bookId != null) {
            highlights.addAll(highlightDao.findByBook(bookId, userId));
        } else {
            for (Book b : books) {
                highlights.addAll(highlightDao.findByBook(b.getId(), userId));
            }
        }

        List<Note> allNotes = new ArrayList<>();
        if (bookId != null) {
            allNotes.addAll(noteDao.findByBook(bookId, userId));
        } else {
            for (Book b : books) {
                allNotes.addAll(noteDao.findByBook(b.getId(), userId));
            }
        }
        Map<UUID, List<Note>> notesByHighlight = allNotes.stream()
                .filter(n -> n.getHighlightId() != null)
                .collect(Collectors.groupingBy(Note::getHighlightId));

        try (PDDocument doc = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float y = 800;
            float margin = 50;
            float width = page.getMediaBox().getWidth() - (2 * margin);

            // Title Banner
            cs.beginText();
            cs.setFont(fontBold, 20);
            cs.newLineAtOffset(margin, y);
            cs.showText("MARGINALIA");
            cs.endText();

            y -= 20;
            cs.beginText();
            cs.setFont(fontOblique, 12);
            cs.newLineAtOffset(margin, y);
            cs.showText("Reading Annotations & Margin Notes Report");
            cs.endText();

            y -= 15;
            cs.setLineWidth(1);
            cs.moveTo(margin, y);
            cs.lineTo(margin + width, y);
            cs.stroke();

            y -= 30;

            if (highlights.isEmpty()) {
                cs.beginText();
                cs.setFont(fontRegular, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("No highlights captured yet.");
                cs.endText();
            } else {
                for (Highlight h : highlights) {
                    if (y < 120) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = 800;
                    }

                    Book b = bookMap.get(h.getBookId());
                    String bookTitle = b != null ? b.getTitle() : "Book";
                    String author = b != null && b.getAuthor() != null ? b.getAuthor() : "";
                    String tag = h.getColorTag() != null ? "#" + h.getColorTag().toUpperCase() : "";
                    String chapter = h.getTextChapterRef() != null ? " · " + h.getTextChapterRef() : "";

                    // Book & Meta header
                    cs.beginText();
                    cs.setFont(fontBold, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(bookTitle + (author.isEmpty() ? "" : " by " + author));
                    cs.setFont(fontRegular, 10);
                    cs.showText("  [" + tag + chapter + "]");
                    cs.endText();

                    y -= 16;

                    // Passage quote text
                    if (h.getPassageText() != null && !h.getPassageText().isBlank()) {
                        List<String> lines = wrapText(h.getPassageText(), 75);
                        for (String line : lines) {
                            if (y < 80) {
                                cs.close();
                                page = new PDPage(PDRectangle.A4);
                                doc.addPage(page);
                                cs = new PDPageContentStream(doc, page);
                                y = 800;
                            }
                            cs.beginText();
                            cs.setFont(fontOblique, 10);
                            cs.newLineAtOffset(margin + 12, y);
                            cs.showText("“" + sanitize(line) + "”");
                            cs.endText();
                            y -= 14;
                        }
                    }

                    // Attached notes
                    List<Note> notes = notesByHighlight.getOrDefault(h.getId(), List.of());
                    for (Note n : notes) {
                        if (n.getBodyMd() != null && !n.getBodyMd().isBlank()) {
                            if (y < 80) {
                                cs.close();
                                page = new PDPage(PDRectangle.A4);
                                doc.addPage(page);
                                cs = new PDPageContentStream(doc, page);
                                y = 800;
                            }
                            cs.beginText();
                            cs.setFont(fontRegular, 9.5f);
                            cs.newLineAtOffset(margin + 20, y);
                            cs.showText("Note: " + sanitize(n.getBodyMd()));
                            cs.endText();
                            y -= 13;
                        }
                    }

                    y -= 15; // spacing between highlights
                }
            }

            cs.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF export: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getExportSummary(UUID userId) {
        int bookCount = bookDao.countByUser(userId);
        List<Highlight> highlights = highlightDao.findRecentForUser(userId, 5000);
        List<Note> notes = noteDao.findRecentForUser(userId, 5000);

        return Map.of(
                "books_count", bookCount,
                "highlights_count", highlights.size(),
                "notes_count", notes.size(),
                "available_formats", List.of("pdf", "csv")
        );
    }

    private static String escapeCsv(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ") + "\"";
    }

    private static String sanitize(String val) {
        if (val == null) return "";
        return val.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static List<String> wrapText(String text, int maxChars) {
        List<String> list = new ArrayList<>();
        if (text == null) return list;
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String w : words) {
            if (current.length() + w.length() + 1 > maxChars) {
                if (current.length() > 0) {
                    list.add(current.toString());
                    current = new StringBuilder();
                }
            }
            if (current.length() > 0) current.append(" ");
            current.append(w);
        }
        if (current.length() > 0) list.add(current.toString());
        return list;
    }
}
