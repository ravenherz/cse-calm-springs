package com.ravenherz.cse.util.imaging;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads camera/file metadata from an uploaded image (JPEG, PNG, HEIC, …)
 * into a string map, including Windows-style labels such as {@code Date taken}.
 */
public final class ImageMetadata {

    public static final String DATE_TAKEN = "Date taken";
    public static final String DIMENSIONS = "Dimensions";
    public static final String CAMERA_MAKER = "Camera maker";
    public static final String CAMERA_MODEL = "Camera model";
    public static final String F_STOP = "F-stop";
    public static final String EXPOSURE_TIME = "Exposure time";
    public static final String ISO_SPEED = "ISO speed";
    public static final String EXPOSURE_BIAS = "Exposure bias";
    public static final String FOCAL_LENGTH = "Focal length";
    public static final String MAX_APERTURE = "Max aperture";
    public static final String METERING_MODE = "Metering mode";
    public static final String FLASH_MODE = "Flash mode";
    public static final String FOCAL_LENGTH_35MM = "35mm focal length";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageMetadata.class);
    private static final DateTimeFormatter STORED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern PIXELS = Pattern.compile("(\\d+)");
    private static final Pattern BYTES_OR_VALUES = Pattern.compile("\\[\\d+ (bytes|values)]");
    private static final List<DateTimeFormatter> LOCAL_DATE_TIMES = List.of(
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"),
            DateTimeFormatter.ofPattern("yyyyMMdd HHmmss"));
    private static final List<DateTimeFormatter> OFFSET_DATE_TIMES = List.of(
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX"),
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
            DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    private static final List<DateTimeFormatter> DATES = List.of(
            DateTimeFormatter.ofPattern("yyyy:MM:dd"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.BASIC_ISO_DATE);
    private static final List<String> DATE_FIELD_KEYS = List.of(
            DATE_TAKEN,
            "Date/Time Original",
            "Date Time Original",
            "Date Created",
            "Create Date",
            "Date/Time Digitized",
            "Date/Time");

    public record Parsed(Map<String, String> fields, LocalDateTime dateTaken) {
        public Parsed {
            fields = fields == null || fields.isEmpty()
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        }
    }

    private ImageMetadata() {
    }

    public static Parsed parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return empty();
        }
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            Map<String, String> fields = readFields(metadata);
            LocalDateTime dateTaken = readDateTaken(metadata, fields);
            if (dateTaken != null) {
                Map<String, String> ordered = new LinkedHashMap<>();
                ordered.put(DATE_TAKEN, formatDateTaken(dateTaken));
                for (Map.Entry<String, String> entry : fields.entrySet()) {
                    if (!DATE_TAKEN.equals(entry.getKey())) {
                        ordered.put(entry.getKey(), entry.getValue());
                    }
                }
                addDimensions(ordered);
                return new Parsed(ordered, dateTaken);
            }
            addDimensions(fields);
            return new Parsed(fields, null);
        } catch (Exception e) {
            LOGGER.warn("Could not read image metadata: {}", e.getMessage());
            return empty();
        }
    }

    public static void applyTo(ResourceData data, Parsed parsed) {
        if (data == null || parsed == null) {
            return;
        }
        Map<String, String> merged = new LinkedHashMap<>(parsed.fields());
        if (data.getMetadata() != null) {
            for (Map.Entry<String, String> entry : data.getMetadata().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    merged.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
        data.setMetadata(merged);
    }

    public static void applyCreationDate(BasicEntity entity, Parsed parsed) {
        if (entity == null || parsed == null || parsed.dateTaken() == null) {
            return;
        }
        entity.setCreationLocalDateTime(parsed.dateTaken());
    }

    public static void setDimensions(ResourceData data, int width, int height) {
        if (data == null || width <= 0 || height <= 0) {
            return;
        }
        data.addMetadata("width", String.valueOf(width));
        data.addMetadata("height", String.valueOf(height));
        data.addMetadata(DIMENSIONS, width + " x " + height);
    }

    public static String formatDateTaken(LocalDateTime dateTaken) {
        if (dateTaken == null) {
            return null;
        }
        return dateTaken.format(STORED);
    }

    public static LocalDateTime parseDateTaken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = stripZoneName(raw.trim());
        for (DateTimeFormatter formatter : OFFSET_DATE_TIMES) {
            try {
                return OffsetDateTime.parse(value, formatter).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
            }
        }
        for (DateTimeFormatter formatter : LOCAL_DATE_TIMES) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        for (DateTimeFormatter formatter : DATES) {
            try {
                return LocalDate.parse(value, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static Parsed empty() {
        return new Parsed(Map.of(), null);
    }

    private static Map<String, String> readFields(Metadata metadata) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (metadata == null) {
            return fields;
        }
        for (Directory directory : metadata.getDirectories()) {
            if (skipDirectory(directory)) {
                continue;
            }
            for (Tag tag : directory.getTags()) {
                String name = windowsLabel(directory, tag);
                String value = tag.getDescription();
                if (skipTag(name, value)) {
                    continue;
                }
                putField(fields, name, value.trim(), directory.getName());
            }
            if (directory instanceof XmpDirectory xmp) {
                readXmp(fields, xmp);
            }
        }
        return fields;
    }

    private static void readXmp(Map<String, String> fields, XmpDirectory xmp) {
        Map<String, String> properties = xmp.getXmpProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String name = windowsName(shortXmpKey(entry.getKey()));
            String value = entry.getValue();
            if (skipTag(name, value)) {
                continue;
            }
            putField(fields, name, value.trim(), "XMP");
        }
    }

    private static LocalDateTime readDateTaken(Metadata metadata, Map<String, String> fields) {
        LocalDateTime fromExif = stringTag(metadata, ExifSubIFDDirectory.class,
                ExifDirectoryBase.TAG_DATETIME_ORIGINAL);
        if (fromExif != null) {
            return fromExif;
        }
        fromExif = stringTag(metadata, ExifIFD0Directory.class, ExifDirectoryBase.TAG_DATETIME_ORIGINAL);
        if (fromExif != null) {
            return fromExif;
        }
        LocalDateTime fromIptc = iptcDateCreated(metadata);
        if (fromIptc != null) {
            return fromIptc;
        }
        for (String key : DATE_FIELD_KEYS) {
            LocalDateTime parsed = parseDateTaken(fields.get(key));
            if (parsed != null) {
                return parsed;
            }
        }
        LocalDateTime digitized = stringTag(metadata, ExifSubIFDDirectory.class,
                ExifDirectoryBase.TAG_DATETIME_DIGITIZED);
        if (digitized != null) {
            return digitized;
        }
        return stringTag(metadata, ExifIFD0Directory.class, ExifDirectoryBase.TAG_DATETIME);
    }

    private static LocalDateTime stringTag(Metadata metadata, Class<? extends Directory> type, int tag) {
        Directory directory = metadata.getFirstDirectoryOfType(type);
        if (directory == null || !directory.containsTag(tag)) {
            return null;
        }
        return parseDateTaken(directory.getString(tag));
    }

    private static LocalDateTime iptcDateCreated(Metadata metadata) {
        IptcDirectory iptc = metadata.getFirstDirectoryOfType(IptcDirectory.class);
        if (iptc == null || !iptc.containsTag(IptcDirectory.TAG_DATE_CREATED)) {
            return null;
        }
        LocalDateTime date = parseDateTaken(iptc.getString(IptcDirectory.TAG_DATE_CREATED));
        if (date == null) {
            return null;
        }
        String time = iptc.getString(IptcDirectory.TAG_TIME_CREATED);
        if (time == null || time.isBlank()) {
            return date;
        }
        String digits = time.trim();
        if (digits.length() >= 6 && digits.substring(0, 6).chars().allMatch(Character::isDigit)) {
            try {
                LocalTime localTime = LocalTime.parse(digits.substring(0, 6),
                        DateTimeFormatter.ofPattern("HHmmss"));
                return LocalDateTime.of(date.toLocalDate(), localTime);
            } catch (DateTimeParseException ignored) {
            }
        }
        return date;
    }

    private static void addDimensions(Map<String, String> fields) {
        if (fields.containsKey(DIMENSIONS)) {
            return;
        }
        String width = firstNumber(fields, "width", "Image Width", "Exif Image Width");
        String height = firstNumber(fields, "height", "Image Height", "Exif Image Height");
        if (width != null && height != null) {
            fields.put(DIMENSIONS, width + " x " + height);
        }
    }

    private static String firstNumber(Map<String, String> fields, String... keys) {
        for (String key : keys) {
            String value = fields.get(key);
            if (value == null || value.isBlank()) {
                continue;
            }
            Matcher matcher = PIXELS.matcher(value);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String windowsLabel(Directory directory, Tag tag) {
        if (directory instanceof ExifDirectoryBase) {
            String fromType = switch (tag.getTagType()) {
                case ExifDirectoryBase.TAG_DATETIME_ORIGINAL -> DATE_TAKEN;
                case ExifDirectoryBase.TAG_MAKE -> CAMERA_MAKER;
                case ExifDirectoryBase.TAG_MODEL -> CAMERA_MODEL;
                case ExifDirectoryBase.TAG_FNUMBER -> F_STOP;
                case ExifDirectoryBase.TAG_EXPOSURE_TIME -> EXPOSURE_TIME;
                case ExifDirectoryBase.TAG_ISO_EQUIVALENT -> ISO_SPEED;
                case ExifDirectoryBase.TAG_EXPOSURE_BIAS -> EXPOSURE_BIAS;
                case ExifDirectoryBase.TAG_FOCAL_LENGTH -> FOCAL_LENGTH;
                case ExifDirectoryBase.TAG_MAX_APERTURE -> MAX_APERTURE;
                case ExifDirectoryBase.TAG_METERING_MODE -> METERING_MODE;
                case ExifDirectoryBase.TAG_FLASH -> FLASH_MODE;
                case ExifDirectoryBase.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH -> FOCAL_LENGTH_35MM;
                default -> null;
            };
            if (fromType != null) {
                return fromType;
            }
        }
        return windowsName(tag.getTagName());
    }

    private static String windowsName(String name) {
        if (name == null) {
            return null;
        }
        return switch (name) {
            case "Date/Time Original", "Date Time Original", "DateTimeOriginal" -> DATE_TAKEN;
            case "Make" -> CAMERA_MAKER;
            case "Model" -> CAMERA_MODEL;
            case "F-Number", "F Number", "FNumber" -> F_STOP;
            case "Exposure Time", "ExposureTime" -> EXPOSURE_TIME;
            case "ISO Speed Ratings", "Photographic Sensitivity", "ISO" -> ISO_SPEED;
            case "Exposure Bias Value", "Exposure Bias" -> EXPOSURE_BIAS;
            case "Focal Length" -> FOCAL_LENGTH;
            case "Max Aperture Value", "Maximum Aperture" -> MAX_APERTURE;
            case "Metering Mode" -> METERING_MODE;
            case "Flash" -> FLASH_MODE;
            case "Focal Length 35", "Focal Length In 35mm Format" -> FOCAL_LENGTH_35MM;
            default -> name;
        };
    }

    private static String shortXmpKey(String key) {
        if (key == null) {
            return null;
        }
        int slash = key.lastIndexOf('/');
        int colon = key.lastIndexOf(':');
        int cut = Math.max(slash, colon);
        return cut >= 0 && cut + 1 < key.length() ? key.substring(cut + 1) : key;
    }

    private static void putField(Map<String, String> fields, String key, String value, String directoryName) {
        String safe = safeKey(key);
        if (safe == null || value == null || value.isBlank()) {
            return;
        }
        String trimmed = value.trim();
        if (!fields.containsKey(safe)) {
            fields.put(safe, trimmed);
            return;
        }
        if (fields.get(safe).equals(trimmed)) {
            return;
        }
        String qualified = safeKey(directoryName + " " + safe);
        if (qualified != null) {
            fields.putIfAbsent(qualified, trimmed);
        }
    }

    private static String safeKey(String key) {
        if (key == null) {
            return null;
        }
        String safe = key.replace('.', ' ').replace('$', ' ').trim();
        return safe.isEmpty() ? null : safe;
    }

    private static boolean skipDirectory(Directory directory) {
        if (directory == null) {
            return true;
        }
        String name = directory.getName();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("huffman") || lower.contains("thumbnail");
    }

    private static boolean skipTag(String name, String value) {
        if (name == null || value == null || value.isBlank()) {
            return true;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("thumbnail") || lower.contains("preview image")
                || lower.contains("maker note") || lower.contains("padding")
                || lower.contains("strip offset") || lower.contains("unknown tag")) {
            return true;
        }
        if (value.length() > 2000) {
            return true;
        }
        return BYTES_OR_VALUES.matcher(value).matches();
    }

    private static String stripZoneName(String value) {
        int paren = value.indexOf(" (");
        if (paren > 0) {
            return value.substring(0, paren).trim();
        }
        return value;
    }
}
