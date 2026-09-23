package com.ravenherz.cse.util.imaging;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * First-page JPEG from an uploaded PDF.
 */
public final class PdfPreviews {

    private static final float RENDER_DPI = 96f;

    private PdfPreviews() {
    }

    public static boolean looksLikePdf(byte[] bytes) {
        if (bytes == null || bytes.length < 5) {
            return false;
        }
        int i = 0;
        int limit = Math.min(bytes.length, 1024);
        while (i < limit && isPdfLead(bytes[i])) {
            i++;
        }
        return i + 4 <= bytes.length
                && bytes[i] == '%'
                && bytes[i + 1] == 'P'
                && bytes[i + 2] == 'D'
                && bytes[i + 3] == 'F';
    }

    public static JpegImages.Encoded firstPage(byte[] pdf, int maxWidth, float quality) throws IOException {
        if (!looksLikePdf(pdf)) {
            throw new IOException("Not a PDF");
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            if (document.getNumberOfPages() < 1) {
                throw new IOException("PDF has no pages");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage page = renderer.renderImageWithDPI(0, RENDER_DPI, ImageType.RGB);
            byte[] jpeg = JpegImages.encode(page, quality);
            if (maxWidth > 0 && page.getWidth() > maxWidth) {
                return JpegImages.toJpeg(jpeg, maxWidth, quality);
            }
            return new JpegImages.Encoded(jpeg, page.getWidth(), page.getHeight());
        }
    }

    private static boolean isPdfLead(byte value) {
        return value == 0 || Character.isWhitespace(value & 0xFF);
    }
}
