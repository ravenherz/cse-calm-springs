package com.ravenherz.cse.util.imaging;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageMetadataTest {

    @Test
    void parseDateTakenAcceptsExifAndIsoForms() {
        assertEquals(LocalDateTime.of(2026, 6, 18, 20, 40, 32),
                ImageMetadata.parseDateTaken("2026:06:18 20:40:32"));
        assertEquals(LocalDateTime.of(2026, 6, 18, 20, 40, 32),
                ImageMetadata.parseDateTaken("2026-06-18 20:40:32"));
        assertEquals(LocalDateTime.of(2026, 6, 18, 20, 40, 32),
                ImageMetadata.parseDateTaken("2026-06-18T20:40:32+04:00"));
        assertEquals(LocalDateTime.of(2026, 6, 18, 20, 40, 32),
                ImageMetadata.parseDateTaken("2026:06:18 20:40:32+04:00"));
        assertEquals(LocalDateTime.of(2026, 6, 18, 0, 0),
                ImageMetadata.parseDateTaken("2026:06:18"));
        assertNull(ImageMetadata.parseDateTaken(""));
        assertNull(ImageMetadata.parseDateTaken(null));
        assertNull(ImageMetadata.parseDateTaken("not-a-date"));
    }

    @Test
    void parseReadsExifIntoWindowsStyleMapAndDateTaken() throws IOException {
        byte[] jpeg = jpegWithExif("2026:06:18 20:40:32", "Apple", "iPhone");
        ImageMetadata.Parsed parsed = ImageMetadata.parse(jpeg);
        assertEquals(LocalDateTime.of(2026, 6, 18, 20, 40, 32), parsed.dateTaken());
        assertEquals("2026-06-18 20:40:32", parsed.fields().get(ImageMetadata.DATE_TAKEN));
        assertEquals("Apple", parsed.fields().get(ImageMetadata.CAMERA_MAKER));
        assertEquals("iPhone", parsed.fields().get(ImageMetadata.CAMERA_MODEL));
        assertFalse(parsed.fields().isEmpty());
    }

    @Test
    void applyToMergesExifAheadOfClientWidthHeight() throws IOException {
        ResourceData data = new ResourceData();
        Map<String, String> client = new LinkedHashMap<>();
        client.put("width", "8");
        client.put("height", "6");
        data.setMetadata(client);
        ImageMetadata.applyTo(data, ImageMetadata.parse(jpegWithExif("2026:06:18 20:40:32", "Apple", "iPhone")));
        assertEquals("2026-06-18 20:40:32", data.getMetadata().get(ImageMetadata.DATE_TAKEN));
        assertEquals("Apple", data.getMetadata().get(ImageMetadata.CAMERA_MAKER));
        assertEquals("8", data.getMetadata().get("width"));
        assertEquals("6", data.getMetadata().get("height"));
    }

    @Test
    void applyCreationDateUsesDateTaken() throws IOException {
        AccountEntity owner = new AccountEntity(new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.OWNER));
        ResourceEntity entity = new ResourceEntity(new ResourceData(), owner);
        LocalDateTime before = entity.selectCreationLocalDateTime();
        ImageMetadata.Parsed parsed = ImageMetadata.parse(jpegWithExif("2026:06:18 20:40:32", "Apple", "iPhone"));
        ImageMetadata.applyCreationDate(entity, parsed);
        assertEquals(LocalDateTime.of(2026, 6, 18, 20, 40, 32), entity.selectCreationLocalDateTime());
        assertTrue(entity.selectCreationLocalDateTime().isBefore(before)
                || !entity.selectCreationLocalDateTime().equals(before));
    }

    @Test
    void parseWithoutExifHasNoDateTaken() throws IOException {
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        byte[] jpeg = JpegImages.encode(image, 0.9f);
        ImageMetadata.Parsed parsed = ImageMetadata.parse(jpeg);
        assertNull(parsed.dateTaken());
        assertNotNull(parsed.fields());
    }

    @Test
    void parseEmptyBytesReturnsEmpty() {
        ImageMetadata.Parsed parsed = ImageMetadata.parse(new byte[0]);
        assertNull(parsed.dateTaken());
        assertTrue(parsed.fields().isEmpty());
    }

    @Test
    void setDimensionsWritesWidthHeightAndWindowsLabel() {
        ResourceData data = new ResourceData();
        ImageMetadata.setDimensions(data, 3024, 4032);
        assertEquals("3024", data.getMetadata().get("width"));
        assertEquals("4032", data.getMetadata().get("height"));
        assertEquals("3024 x 4032", data.getMetadata().get(ImageMetadata.DIMENSIONS));
    }

    private static byte[] jpegWithExif(String dateTaken, String make, String model) throws IOException {
        byte[] jpeg = JpegImages.encode(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), 0.9f);
        byte[] app1 = exifApp1(dateTaken, make, model);
        byte[] out = new byte[jpeg.length + app1.length];
        out[0] = (byte) 0xFF;
        out[1] = (byte) 0xD8;
        System.arraycopy(app1, 0, out, 2, app1.length);
        System.arraycopy(jpeg, 2, out, 2 + app1.length, jpeg.length - 2);
        return out;
    }

    private static byte[] exifApp1(String dateTaken, String make, String model) {
        byte[] tiff = tiffExif(dateTaken, make, model);
        int payload = 6 + tiff.length;
        ByteBuffer buf = ByteBuffer.allocate(4 + payload);
        buf.put((byte) 0xFF);
        buf.put((byte) 0xE1);
        buf.putShort((short) (2 + payload));
        buf.put(new byte[] {'E', 'x', 'i', 'f', 0, 0});
        buf.put(tiff);
        return buf.array();
    }

    private static byte[] tiffExif(String dateTaken, String make, String model) {
        List<AsciiTag> ifd0 = new ArrayList<>();
        ifd0.add(new AsciiTag(0x010F, make + '\0'));
        ifd0.add(new AsciiTag(0x0110, model + '\0'));
        List<AsciiTag> exif = List.of(new AsciiTag(0x9003, dateTaken + '\0'));
        int ifd0Count = ifd0.size() + 1;
        int ifd0At = 8;
        int ifd0Size = 2 + 12 * ifd0Count + 4;
        int exifIfdAt = ifd0At + ifd0Size;
        int exifIfdSize = 2 + 12 * exif.size() + 4;
        int heapAt = exifIfdAt + exifIfdSize;
        List<Integer> ifd0Heap = new ArrayList<>();
        int cursor = heapAt;
        for (AsciiTag tag : ifd0) {
            ifd0Heap.add(cursor);
            cursor += bytes(tag.value).length;
        }
        List<Integer> exifHeap = new ArrayList<>();
        for (AsciiTag tag : exif) {
            exifHeap.add(cursor);
            cursor += bytes(tag.value).length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(cursor).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 'I');
        buffer.put((byte) 'I');
        buffer.putShort((short) 42);
        buffer.putInt(ifd0At);
        buffer.position(ifd0At);
        buffer.putShort((short) ifd0Count);
        for (int i = 0; i < ifd0.size(); i++) {
            putAscii(buffer, ifd0.get(i), ifd0Heap.get(i));
        }
        buffer.putShort((short) 0x8769);
        buffer.putShort((short) 4);
        buffer.putInt(1);
        buffer.putInt(exifIfdAt);
        buffer.putInt(0);
        buffer.position(exifIfdAt);
        buffer.putShort((short) exif.size());
        for (int i = 0; i < exif.size(); i++) {
            putAscii(buffer, exif.get(i), exifHeap.get(i));
        }
        buffer.putInt(0);
        buffer.position(heapAt);
        for (AsciiTag tag : ifd0) {
            buffer.put(bytes(tag.value));
        }
        for (AsciiTag tag : exif) {
            buffer.put(bytes(tag.value));
        }
        return buffer.array();
    }

    private static void putAscii(ByteBuffer buffer, AsciiTag tag, int offset) {
        byte[] value = bytes(tag.value);
        buffer.putShort((short) tag.id);
        buffer.putShort((short) 2);
        buffer.putInt(value.length);
        if (value.length <= 4) {
            byte[] inline = new byte[4];
            System.arraycopy(value, 0, inline, 0, value.length);
            buffer.put(inline);
        } else {
            buffer.putInt(offset);
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private record AsciiTag(int id, String value) {
    }
}
