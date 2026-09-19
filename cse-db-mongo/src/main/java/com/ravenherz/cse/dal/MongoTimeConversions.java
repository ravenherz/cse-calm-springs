package com.ravenherz.cse.dal;

import com.ravenherz.cse.dal.dto.basic.AccessRule;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JSR-310 mapping that does not reflect into {@code java.time} (Java 16+ modules).
 * Morphia stored {@link LocalDateTime} as BSON Date (UTC) or as a nested document.
 */
public final class MongoTimeConversions {

    private MongoTimeConversions() {
    }

    public static MongoCustomConversions create() {
        return new MongoCustomConversions(List.of(
                new DateToLocalDateTimeConverter(),
                new LocalDateTimeToDateConverter(),
                new DocumentToLocalDateTimeConverter(),
                new StringToSecurityLevelConverter(),
                new StringToAccessRuleConverter(),
                new DocumentToAccessRuleConverter(),
                new AccessRuleToDocumentConverter()));
    }

    static LocalDateTime fromDate(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    static LocalDateTime fromDocument(Document document) {
        if (document.containsKey("date") && document.containsKey("time")) {
            return LocalDateTime.of(toLocalDate(document.get("date")), toLocalTime(document.get("time")));
        }
        Integer year = integer(document, "year");
        Integer month = monthNumber(document.get("month"));
        if (month == null) {
            month = integer(document, "monthValue");
        }
        Integer day = integer(document, "dayOfMonth");
        Integer hour = integer(document, "hour");
        Integer minute = integer(document, "minute");
        Integer second = integer(document, "second");
        Integer nano = integer(document, "nano");
        if (year != null && month != null && day != null) {
            return LocalDateTime.of(year, month, day,
                    hour == null ? 0 : hour,
                    minute == null ? 0 : minute,
                    second == null ? 0 : second,
                    nano == null ? 0 : nano);
        }
        throw new IllegalArgumentException("Cannot read LocalDateTime from " + document);
    }

    private static LocalDate toLocalDate(Object raw) {
        if (raw instanceof LocalDate localDate) {
            return localDate;
        }
        if (raw instanceof Date date) {
            return fromDate(date).toLocalDate();
        }
        if (raw instanceof Document document) {
            Integer year = integer(document, "year");
            Integer month = monthNumber(document.get("month"));
            if (month == null) {
                month = integer(document, "monthValue");
            }
            Integer day = integer(document, "dayOfMonth");
            if (year != null && month != null && day != null) {
                return LocalDate.of(year, month, day);
            }
        }
        throw new IllegalArgumentException("Cannot read LocalDate from " + raw);
    }

    private static LocalTime toLocalTime(Object raw) {
        if (raw instanceof LocalTime localTime) {
            return localTime;
        }
        if (raw instanceof Date date) {
            return fromDate(date).toLocalTime();
        }
        if (raw instanceof Document document) {
            Integer hour = integer(document, "hour");
            Integer minute = integer(document, "minute");
            Integer second = integer(document, "second");
            Integer nano = integer(document, "nano");
            return LocalTime.of(
                    hour == null ? 0 : hour,
                    minute == null ? 0 : minute,
                    second == null ? 0 : second,
                    nano == null ? 0 : nano);
        }
        throw new IllegalArgumentException("Cannot read LocalTime from " + raw);
    }

    private static Integer integer(Document document, String key) {
        Object value = document.get(key);
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static Integer monthNumber(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof Month month) {
            return month.getValue();
        }
        if (raw instanceof String name) {
            try {
                return Month.valueOf(name).getValue();
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        if (raw instanceof Document document) {
            Integer value = integer(document, "value");
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @ReadingConverter
    static final class DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
        @Override
        public LocalDateTime convert(Date source) {
            return fromDate(source);
        }
    }

    @WritingConverter
    static final class LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
        @Override
        public Date convert(LocalDateTime source) {
            return Date.from(source.toInstant(ZoneOffset.UTC));
        }
    }

    @ReadingConverter
    static final class DocumentToLocalDateTimeConverter implements Converter<Document, LocalDateTime> {
        @Override
        public LocalDateTime convert(Document source) {
            return fromDocument(source);
        }
    }

    @ReadingConverter
    static final class StringToSecurityLevelConverter implements Converter<String, SecurityLevel> {
        @Override
        public SecurityLevel convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }
            String name = source.trim();
            if ("GUIDE".equals(name)) {
                return SecurityLevel.INACTIVE_USER;
            }
            return SecurityLevel.valueOf(name);
        }
    }

    @ReadingConverter
    static final class StringToAccessRuleConverter implements Converter<String, AccessRule> {
        @Override
        public AccessRule convert(String source) {
            if (source == null || source.isBlank()) {
                return AccessRule.inheritAll();
            }
            String name = source.trim();
            if ("GUIDE".equals(name)) {
                return AccessRule.fromLegacy(SecurityLevel.OPERATOR);
            }
            try {
                return AccessRule.fromLegacy(SecurityLevel.valueOf(name));
            } catch (IllegalArgumentException ex) {
                return AccessRule.inheritAll();
            }
        }
    }

    @ReadingConverter
    static final class DocumentToAccessRuleConverter implements Converter<Document, AccessRule> {
        @Override
        public AccessRule convert(Document source) {
            if (source == null) {
                return AccessRule.inheritAll();
            }
            AccessRule rule = new AccessRule();
            Object inherit = source.get("inherit");
            rule.setInherit(!(inherit instanceof Boolean) || (Boolean) inherit);
            rule.setRoleIds(withoutRetired(stringList(source.get("roleIds"))));
            rule.setAccountIds(stringList(source.get("accountIds")));
            Object legacy = source.get("legacyThreshold");
            if (legacy != null) {
                String name = legacy.toString();
                rule.setLegacyThreshold("GUIDE".equals(name) ? "OPERATOR" : name);
            }
            return rule;
        }
    }

    @WritingConverter
    static final class AccessRuleToDocumentConverter implements Converter<AccessRule, Document> {
        @Override
        public Document convert(AccessRule source) {
            Document document = new Document();
            document.put("inherit", source != null && source.isInherit());
            document.put("roleIds", source == null ? List.of() : withoutRetired(source.getRoleIds()));
            document.put("accountIds", source == null ? List.of() : source.getAccountIds());
            return document;
        }
    }

    private static List<String> withoutRetired(List<String> ids) {
        List<String> out = new ArrayList<>();
        for (String id : ids) {
            if (!RoleSeeds.isRetiredSlug(id)) {
                out.add(id);
            }
        }
        return out;
    }

    private static List<String> stringList(Object raw) {
        List<String> out = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return out;
        }
        for (Object item : list) {
            if (item != null) {
                String text = item.toString().trim();
                if (!text.isEmpty()) {
                    out.add(text);
                }
            }
        }
        return out;
    }
}
