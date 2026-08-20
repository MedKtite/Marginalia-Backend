package com.marginalia.dev.services.metadata;
import com.marginalia.dev.entities.Book;
import com.marginalia.dev.entities.BookMetadataCache;
import com.marginalia.dev.repo.dao.BookDao;
import com.marginalia.dev.repo.dao.BookMetadataCacheDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Service
public class MetadataResolver {
    private final BookDao bookDao;
    private final BookMetadataCacheDao cacheDao;
    private final List<MetadataClient> clients;

    @Autowired
    public MetadataResolver(BookDao bookDao,
                            BookMetadataCacheDao cacheDao,
                            GutendexClient gutendexClient,
                            OpenLibraryClient openLibraryClient,
                            GoogleBooksClient googleBooksClient,
                            WikidataClient wikidataClient) {
        this.bookDao = bookDao;
        this.cacheDao = cacheDao;
        this.clients = List.of(
                gutendexClient,
                openLibraryClient,
                googleBooksClient,
                wikidataClient);
    }

    public Book enrich(Book book) {
        return resolve(book.getIsbn13(), book.getTitle(), book.getAuthor())
                .filter(MetadataResult::hasAnyValue)
                .flatMap(result -> bookDao.patchMetadata(
                        book.getId(), book.getUserId(), result))
                .orElse(book);
    }

    /** Resolves metadata for persisted books and transient catalog entries. */
    public Optional<MetadataResult> resolve(String isbn13, String title, String author) {
        final String isbn = blankToNull(isbn13);
        if (isbn != null) {
            final Optional<BookMetadataCache> cached = cacheDao.findFresh(isbn);
            if (cached.isPresent()) return cached.map(this::fromCache);
        }

        final List<java.util.concurrent.CompletableFuture<Optional<MetadataResult>>> futures = clients.stream()
                .map(client -> java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return client.find(isbn, title, author);
                            } catch (Exception e) {
                                return Optional.<MetadataResult>empty();
                            }
                        }))
                .toList();

        final List<MetadataResult> results = new ArrayList<>();
        for (var f : futures) {
            try {
                f.join().ifPresent(results::add);
            } catch (Exception ignored) {}
        }
        final MetadataResult merged = merge(results);
        if (merged != null && merged.isbn13() != null) {
            cacheDao.upsert(toCache(merged));
        }
        return Optional.ofNullable(merged);
    }
    private static MetadataResult merge(List<MetadataResult> rs) { if (rs.isEmpty()) return null; MetadataResult f=rs.get(0); String isbn=f.isbn13(),title=f.title(),publisher=f.publisher(),description=f.description(),cover=f.coverUrl(),source=f.source(); Integer year=f.publishedYear(),pages=f.pageCount(); List<String> authors=f.authors(); for(MetadataResult r:rs){if(isbn==null)isbn=r.isbn13();if(title==null)title=r.title();if(publisher==null)publisher=r.publisher();if(description==null)description=r.description();if(cover==null)cover=r.coverUrl();if(year==null)year=r.publishedYear();if(pages==null)pages=r.pageCount();if((authors==null||authors.isEmpty())&&r.authors()!=null&&!r.authors().isEmpty())authors=r.authors();} return new MetadataResult(source,isbn,title,authors,publisher,year,pages,description,cover,f.raw()); }
    private BookMetadataCache toCache(MetadataResult r) { BookMetadataCache c=new BookMetadataCache(); c.setIsbn13(r.isbn13());c.setSource(r.source());c.setTitle(r.title());c.setAuthors(r.authors()==null?null:r.authors().toArray(String[]::new));c.setPublisher(r.publisher());c.setPublishedYear(r.publishedYear());c.setPageCount(r.pageCount());c.setDescription(r.description());c.setCoverUrl(r.coverUrl());c.setRaw(r.raw());return c; }
    private MetadataResult fromCache(BookMetadataCache c) { return new MetadataResult(c.getSource(),c.getIsbn13(),c.getTitle(),c.getAuthors()==null?List.of():List.of(c.getAuthors()),c.getPublisher(),c.getPublishedYear(),c.getPageCount(),c.getDescription(),c.getCoverUrl(),c.getRaw()); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }
}
