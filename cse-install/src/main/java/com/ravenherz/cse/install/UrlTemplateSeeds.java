package com.ravenherz.cse.install;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.UrlTemplateService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.UrlTemplateEntity;
import com.ravenherz.cse.dal.dto.basic.UrlTemplateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * First-boot seed of shipped URL templates. Inserts only when the collection is empty.
 * Access rules come from {@link UrlTemplateEntity}'s default inherit settings.
 */
@Component
class UrlTemplateSeeds {

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlTemplateSeeds.class);
    private static final ObjectMapper JSON = new JsonMapper();
    private static final TypeReference<List<Map<String, Object>>> DOC_ARRAY = new TypeReference<>() {};
    private static final String INIT_PATH = "/static/content-private/cse-url-templates-init.json";
    private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9-]*$");

    private final ServiceProvider serviceProvider;

    public UrlTemplateSeeds(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public int ensureSeeded() {
        try {
            UrlTemplateService templates = serviceProvider.getUrlTemplateService();
            List<UrlTemplateEntity> existing = templates == null ? null : templates.getAllUrlTemplates();
            if (templates == null || (existing != null && !existing.isEmpty())) {
                return 0;
            }
            AccountEntity creator = firstAccount();
            if (creator == null) {
                return 0;
            }
            List<UrlTemplateEntity> seeded = loadInit(creator);
            int inserted = 0;
            for (UrlTemplateEntity entity : seeded) {
                if (templates.getByUrlTemplateId(entity.getUrlTemplateId()) != null) {
                    continue;
                }
                try {
                    templates.insert(entity);
                    inserted++;
                } catch (RuntimeException ex) {
                    LOGGER.warn("Could not seed URL template '{}': {}", entity.getUrlTemplateId(),
                            ex.getMessage());
                }
            }
            if (inserted > 0) {
                LOGGER.info("Seeded {} URL templates from {}", inserted, INIT_PATH);
            }
            return inserted;
        } catch (Exception ex) {
            LOGGER.warn("Could not seed URL templates: {}", ex.getMessage());
            return 0;
        }
    }

    static List<UrlTemplateEntity> loadInit(AccountEntity creator) throws IOException {
        try (InputStream in = ClasspathFiles.open(INIT_PATH)) {
            if (in == null) {
                throw new IOException("Missing classpath " + INIT_PATH);
            }
            return parse(in, creator);
        }
    }

    static List<UrlTemplateEntity> parse(InputStream in, AccountEntity creator) throws IOException {
        List<Map<String, Object>> docs = JSON.readValue(in, DOC_ARRAY);
        if (docs == null) {
            return List.of();
        }
        List<UrlTemplateEntity> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> doc : docs) {
            UrlTemplateEntity entity = toEntity(doc, creator);
            if (entity == null || !seen.add(entity.getUrlTemplateId())) {
                continue;
            }
            out.add(entity);
        }
        return out;
    }

    private AccountEntity firstAccount() {
        AccountService accounts = serviceProvider.getAccountService();
        if (accounts == null) {
            return null;
        }
        List<AccountEntity> all = accounts.getAllAccounts();
        if (all == null || all.isEmpty()) {
            return null;
        }
        return all.get(0);
    }

    private static UrlTemplateEntity toEntity(Map<String, Object> doc, AccountEntity creator) {
        if (doc == null) {
            return null;
        }
        String id = normalizeId(text(doc.get("urlTemplateId")));
        if (!validId(id)) {
            return null;
        }
        Map<String, Object> dataDoc = map(doc.get("urlTemplateData"));
        if (dataDoc == null) {
            return null;
        }
        String pattern = text(dataDoc.get("urlPattern"));
        if (pattern == null || pattern.isBlank()) {
            return null;
        }
        UrlTemplateData data = new UrlTemplateData();
        data.setUrlPattern(pattern.trim());
        data.setUrlDefaultText(blankToNull(text(dataDoc.get("urlDefaultText"))));
        data.setUrlImage(blankToNull(text(dataDoc.get("urlImage"))));
        return new UrlTemplateEntity(id, data, creator);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> nested) {
            return (Map<String, Object>) nested;
        }
        return null;
    }

    private static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean validId(String urlTemplateId) {
        return urlTemplateId != null && !urlTemplateId.isBlank() && ID.matcher(urlTemplateId).matches();
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
