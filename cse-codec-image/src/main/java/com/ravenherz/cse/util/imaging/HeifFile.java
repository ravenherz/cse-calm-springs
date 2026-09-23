package com.ravenherz.cse.util.imaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the ISOBMFF boxes of a HEIF file: which item is the primary image, where its
 * coded bytes live, and the properties needed to rebuild it (decoder config, tile grid,
 * rotation, mirroring).
 * <p>
 * Only the metadata is read here. Pixels are never decoded.
 */
final class HeifFile {

    /** Coded tiles laid out left to right, top to bottom, then cropped to the output size. */
    record Grid(int columns, int rows, int width, int height) {
    }

    /** {@code irot} or {@code imir}, in the order the file associates them. */
    record Transform(String type, int value) {
    }

    private record Property(String type, int start, int end) {
    }

    private record Location(int constructionMethod, List<long[]> extents) {
    }

    private final byte[] data;
    private final Map<Integer, String> itemTypes = new LinkedHashMap<>();
    private final Map<Integer, Map<String, List<Integer>>> references = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> associations = new LinkedHashMap<>();
    private final Map<Integer, Location> locations = new LinkedHashMap<>();
    private final List<Property> properties = new ArrayList<>();
    private byte[] itemData = new byte[0];
    private int primaryItem = -1;

    private HeifFile(byte[] data) {
        this.data = data;
    }

    static HeifFile parse(byte[] data) throws IOException {
        if (data == null || data.length < 16) {
            throw new IOException("HEIC file is empty");
        }
        HeifFile file = new HeifFile(data);
        file.readTopLevel();
        if (file.primaryItem < 0) {
            throw new IOException("HEIC file has no primary image");
        }
        return file;
    }

    int primaryItemId() {
        return primaryItem;
    }

    String itemType(int itemId) {
        return itemTypes.get(itemId);
    }

    List<Integer> derivedFrom(int itemId) {
        return references.getOrDefault(itemId, Map.of()).getOrDefault("dimg", List.of());
    }

    byte[] itemData(int itemId) throws IOException {
        Location location = locations.get(itemId);
        if (location == null) {
            throw new IOException("HEIC item " + itemId + " has no location");
        }
        byte[] source = location.constructionMethod() == 1 ? itemData : data;
        int total = 0;
        for (long[] extent : location.extents()) {
            total = Math.addExact(total, Math.toIntExact(extent[1]));
        }
        byte[] out = new byte[total];
        int at = 0;
        for (long[] extent : location.extents()) {
            int offset = Math.toIntExact(extent[0]);
            int length = Math.toIntExact(extent[1]);
            if (offset < 0 || length < 0 || offset + length > source.length) {
                throw new IOException("HEIC item " + itemId + " points outside the file");
            }
            System.arraycopy(source, offset, out, at, length);
            at += length;
        }
        return out;
    }

    Grid grid(int itemId) throws IOException {
        byte[] descriptor = itemData(itemId);
        if (descriptor.length < 8) {
            throw new IOException("HEIC grid descriptor is truncated");
        }
        int rows = (descriptor[2] & 0xFF) + 1;
        int columns = (descriptor[3] & 0xFF) + 1;
        int fieldLength = ((descriptor[1] & 0x01) + 1) * 2;
        if (descriptor.length < 4 + 2 * fieldLength) {
            throw new IOException("HEIC grid descriptor is truncated");
        }
        int width = readUnsigned(descriptor, 4, fieldLength);
        int height = readUnsigned(descriptor, 4 + fieldLength, fieldLength);
        if (width <= 0 || height <= 0) {
            throw new IOException("HEIC grid has no size");
        }
        return new Grid(columns, rows, width, height);
    }

    /** The {@code hvcC} decoder configuration record for an item. */
    byte[] decoderConfig(int itemId) throws IOException {
        Property property = property(itemId, "hvcC");
        if (property == null) {
            throw new IOException("HEIC item " + itemId + " has no HEVC decoder config");
        }
        byte[] out = new byte[property.end() - property.start()];
        System.arraycopy(data, property.start(), out, 0, out.length);
        return out;
    }

    /** {@code ispe} width and height, or null when the item declares none. */
    int[] size(int itemId) {
        Property property = property(itemId, "ispe");
        if (property == null || property.end() - property.start() < 12) {
            return null;
        }
        return new int[] {readInt(property.start() + 4), readInt(property.start() + 8)};
    }

    List<Transform> transforms(int itemId) {
        List<Transform> out = new ArrayList<>();
        for (int index : associations.getOrDefault(itemId, List.of())) {
            if (index < 1 || index > properties.size()) {
                continue;
            }
            Property property = properties.get(index - 1);
            if (property.end() <= property.start()) {
                continue;
            }
            if ("irot".equals(property.type())) {
                out.add(new Transform("irot", data[property.start()] & 0x03));
            } else if ("imir".equals(property.type())) {
                out.add(new Transform("imir", data[property.start()] & 0x01));
            }
        }
        return out;
    }

    private Property property(int itemId, String type) {
        for (int index : associations.getOrDefault(itemId, List.of())) {
            if (index < 1 || index > properties.size()) {
                continue;
            }
            Property property = properties.get(index - 1);
            if (property.type().equals(type)) {
                return property;
            }
        }
        return null;
    }

    private void readTopLevel() throws IOException {
        for (int[] box : boxes(0, data.length)) {
            if ("meta".equals(type(box))) {
                readMeta(box[1] + 4, box[2]);
                return;
            }
        }
        throw new IOException("HEIC file has no meta box");
    }

    private void readMeta(int start, int end) throws IOException {
        for (int[] box : boxes(start, end)) {
            switch (type(box)) {
                case "pitm" -> primaryItem = version(box) == 0
                        ? readShort(box[1] + 4)
                        : readInt(box[1] + 4);
                case "iinf" -> readItemInfo(box);
                case "iref" -> readItemReferences(box);
                case "iprp" -> readItemProperties(box);
                case "iloc" -> readItemLocations(box);
                case "idat" -> itemData = slice(box[1], box[2]);
                default -> {
                }
            }
        }
    }

    private void readItemInfo(int[] box) {
        int cursor = box[1] + 4 + (version(box) == 0 ? 2 : 4);
        for (int[] entry : boxes(cursor, box[2])) {
            if (!"infe".equals(type(entry)) || version(entry) < 2) {
                continue;
            }
            int at = entry[1] + 4;
            int itemId;
            if (version(entry) == 2) {
                itemId = readShort(at);
                at += 2;
            } else {
                itemId = readInt(at);
                at += 4;
            }
            at += 2;
            itemTypes.put(itemId, new String(data, at, 4, java.nio.charset.StandardCharsets.ISO_8859_1));
        }
    }

    private void readItemReferences(int[] box) {
        boolean large = version(box) != 0;
        for (int[] entry : boxes(box[1] + 4, box[2])) {
            int at = entry[1];
            int from;
            if (large) {
                from = readInt(at);
                at += 4;
            } else {
                from = readShort(at);
                at += 2;
            }
            int count = readShort(at);
            at += 2;
            List<Integer> to = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                if (large) {
                    to.add(readInt(at));
                    at += 4;
                } else {
                    to.add(readShort(at));
                    at += 2;
                }
            }
            references.computeIfAbsent(from, key -> new LinkedHashMap<>()).put(type(entry), to);
        }
    }

    private void readItemProperties(int[] box) {
        for (int[] child : boxes(box[1], box[2])) {
            if ("ipco".equals(type(child))) {
                for (int[] property : boxes(child[1], child[2])) {
                    properties.add(new Property(type(property), property[1], property[2]));
                }
            } else if ("ipma".equals(type(child))) {
                readPropertyAssociations(child);
            }
        }
    }

    private void readPropertyAssociations(int[] box) {
        boolean largeIndex = (readInt(box[1]) & 0x000001) != 0;
        boolean largeId = version(box) >= 1;
        int at = box[1] + 4;
        int count = readInt(at);
        at += 4;
        for (int i = 0; i < count && at < box[2]; i++) {
            int itemId;
            if (largeId) {
                itemId = readInt(at);
                at += 4;
            } else {
                itemId = readShort(at);
                at += 2;
            }
            int associationCount = data[at] & 0xFF;
            at++;
            List<Integer> indexes = new ArrayList<>(associationCount);
            for (int j = 0; j < associationCount && at < box[2]; j++) {
                if (largeIndex) {
                    indexes.add(readShort(at) & 0x7FFF);
                    at += 2;
                } else {
                    indexes.add(data[at] & 0x7F);
                    at++;
                }
            }
            associations.put(itemId, indexes);
        }
    }

    private void readItemLocations(int[] box) {
        int version = version(box);
        int at = box[1] + 4;
        int offsetSize = (data[at] & 0xFF) >> 4;
        int lengthSize = data[at] & 0x0F;
        int baseOffsetSize = (data[at + 1] & 0xFF) >> 4;
        int indexSize = version >= 1 ? (data[at + 1] & 0x0F) : 0;
        at += 2;
        int count;
        if (version < 2) {
            count = readShort(at);
            at += 2;
        } else {
            count = readInt(at);
            at += 4;
        }
        for (int i = 0; i < count && at < box[2]; i++) {
            int itemId;
            if (version < 2) {
                itemId = readShort(at);
                at += 2;
            } else {
                itemId = readInt(at);
                at += 4;
            }
            int constructionMethod = 0;
            if (version == 1 || version == 2) {
                constructionMethod = readShort(at) & 0x0F;
                at += 2;
            }
            at += 2;
            long baseOffset = readUnsigned(data, at, baseOffsetSize);
            at += baseOffsetSize;
            int extentCount = readShort(at);
            at += 2;
            List<long[]> extents = new ArrayList<>(extentCount);
            for (int j = 0; j < extentCount; j++) {
                at += indexSize;
                long offset = readUnsigned(data, at, offsetSize);
                at += offsetSize;
                long length = readUnsigned(data, at, lengthSize);
                at += lengthSize;
                extents.add(new long[] {baseOffset + offset, length});
            }
            locations.put(itemId, new Location(constructionMethod, extents));
        }
    }

    /** {@code {contentStart, headerEnd, boxEnd}} for every box between {@code start} and {@code end}. */
    private List<int[]> boxes(int start, int end) {
        List<int[]> out = new ArrayList<>();
        int at = start;
        while (at + 8 <= end) {
            long size = readInt(at) & 0xFFFFFFFFL;
            int header = 8;
            if (size == 1) {
                if (at + 16 > end) {
                    break;
                }
                size = readLong(at + 8);
                header = 16;
            } else if (size == 0) {
                size = end - at;
            }
            if (size < header || at + size > end) {
                break;
            }
            out.add(new int[] {at, at + header, (int) (at + size)});
            at += (int) size;
        }
        return out;
    }

    private String type(int[] box) {
        return new String(data, box[0] + 4, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    private int version(int[] box) {
        return data[box[1]] & 0xFF;
    }

    private byte[] slice(int start, int end) {
        byte[] out = new byte[end - start];
        System.arraycopy(data, start, out, 0, out.length);
        return out;
    }

    private int readShort(int at) {
        return ((data[at] & 0xFF) << 8) | (data[at + 1] & 0xFF);
    }

    private int readInt(int at) {
        return ((data[at] & 0xFF) << 24) | ((data[at + 1] & 0xFF) << 16)
                | ((data[at + 2] & 0xFF) << 8) | (data[at + 3] & 0xFF);
    }

    private long readLong(int at) {
        long high = readInt(at) & 0xFFFFFFFFL;
        long low = readInt(at + 4) & 0xFFFFFFFFL;
        return (high << 32) | low;
    }

    private static int readUnsigned(byte[] source, int at, int size) {
        int value = 0;
        for (int i = 0; i < size; i++) {
            value = (value << 8) | (source[at + i] & 0xFF);
        }
        return value;
    }
}
