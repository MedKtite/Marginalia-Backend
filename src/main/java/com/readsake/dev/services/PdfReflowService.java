package com.readsake.dev.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Creates a compact reflowable EPUB from a text-based uploaded PDF. */
@Service
public class PdfReflowService {

    private static final Logger log = LoggerFactory.getLogger(PdfReflowService.class);
    private static final int MIN_TEXT_LENGTH = 200;

    public byte[] convert(byte[] pdf, String title, String author) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document).trim();
            if (text.length() < MIN_TEXT_LENGTH) {
                log.info("PDF has insufficient extractable text; retaining page reader");
                return null;
            }
            return epub(title, author, text);
        } catch (Exception e) {
            log.warn("PDF reflow conversion failed; retaining page reader: {}", e.getMessage());
            return null;
        }
    }

    private byte[] epub(String title, String author, String text) throws Exception {
        String safeTitle = xml(title == null || title.isBlank() ? "Untitled" : title);
        String safeAuthor = xml(author == null ? "" : author);
        String identifier = "urn:uuid:" + UUID.randomUUID();
        String content = paragraphs(text);

        String container = """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles><rootfile full-path="OEBPS/package.opf" media-type="application/oebps-package+xml"/></rootfiles>
                </container>
                """;
        String nav = """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
                <head><title>Contents</title></head><body><nav epub:type="toc"><ol><li><a href="content.xhtml">%s</a></li></ol></nav></body></html>
                """.formatted(safeTitle);
        String xhtml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml"><head><title>%s</title>
                <style>html,body{margin:0;padding:0;overflow:hidden}body{font-family:serif;line-height:1.6}h1{line-height:1.2}p{margin:0 0 1em}</style>
                </head><body><h1>%s</h1>%s</body></html>
                """.formatted(safeTitle, safeTitle, content);
        String opf = """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:identifier id="book-id">%s</dc:identifier><dc:title>%s</dc:title>
                    <dc:language>en</dc:language><dc:creator>%s</dc:creator>
                    <meta property="dcterms:modified">2026-01-01T00:00:00Z</meta>
                  </metadata>
                  <manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                    <item id="content" href="content.xhtml" media-type="application/xhtml+xml"/></manifest>
                  <spine><itemref idref="content"/></spine>
                </package>
                """.formatted(identifier, safeTitle, safeAuthor);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            byte[] mime = "application/epub+zip".getBytes(StandardCharsets.US_ASCII);
            CRC32 crc = new CRC32();
            crc.update(mime);
            ZipEntry mimetype = new ZipEntry("mimetype");
            mimetype.setMethod(ZipEntry.STORED);
            mimetype.setSize(mime.length);
            mimetype.setCompressedSize(mime.length);
            mimetype.setCrc(crc.getValue());
            zip.putNextEntry(mimetype);
            zip.write(mime);
            zip.closeEntry();
            entry(zip, "META-INF/container.xml", container);
            entry(zip, "OEBPS/package.opf", opf);
            entry(zip, "OEBPS/nav.xhtml", nav);
            entry(zip, "OEBPS/content.xhtml", xhtml);
        }
        return out.toByteArray();
    }

    private static void entry(ZipOutputStream zip, String name, String value) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String paragraphs(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] blocks = normalized.split("\\n\\s*\\n+");
        StringBuilder html = new StringBuilder();
        for (String block : blocks) {
            String clean = block.replaceAll("(?<=\\w)-\\n(?=\\w)", "")
                    .replaceAll("\\s*\\n\\s*", " ").trim();
            if (!clean.isEmpty()) html.append("<p>").append(xml(clean)).append("</p>");
        }
        return html.toString();
    }

    private static String xml(String value) {
        StringBuilder valid = new StringBuilder(value.length());
        value.codePoints().filter(PdfReflowService::isValidXmlCharacter)
                .forEach(valid::appendCodePoint);
        return valid.toString().replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static boolean isValidXmlCharacter(int codePoint) {
        return codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD
                || codePoint >= 0x20 && codePoint <= 0xD7FF
                || codePoint >= 0xE000 && codePoint <= 0xFFFD
                || codePoint >= 0x10000 && codePoint <= 0x10FFFF;
    }
}
