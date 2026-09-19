package com.ravenherz.cse.util.imaging;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfPreviewsTest {

    @Test
    void firstPageRendersJpeg() throws IOException {
        byte[] pdf = solidPage(200, 280, Color.RED);
        assertTrue(PdfPreviews.looksLikePdf(pdf));
        JpegImages.Encoded jpeg = PdfPreviews.firstPage(pdf, 140, 0.9f);
        assertTrue(jpeg.width() > 0);
        assertTrue(jpeg.height() > 0);
        assertTrue(jpeg.width() <= 140);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg.bytes()));
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
    }

    @Test
    void rejectsNonPdf() {
        assertFalse(PdfPreviews.looksLikePdf("not-a-pdf".getBytes()));
        assertThrows(IOException.class, () -> PdfPreviews.firstPage("not-a-pdf".getBytes(), 100, 0.9f));
    }

    private static byte[] solidPage(float width, float height, Color color) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(width, height));
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.setNonStrokingColor(color);
                stream.addRect(0, 0, width, height);
                stream.fill();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
