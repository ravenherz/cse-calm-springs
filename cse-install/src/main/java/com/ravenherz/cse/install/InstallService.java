package com.ravenherz.cse.install;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dao.CategoryService;
import com.ravenherz.cse.dal.dao.RoleMatrixService;
import com.ravenherz.cse.dal.dao.RoleService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.CategoryEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.CategoryData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.dal.role.RoleSeeds;
import com.ravenherz.cse.security.AccountRoles;
import com.ravenherz.cse.util.PasswordHashes;
import com.ravenherz.cse.util.staticapps.StaticAppDeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InstallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallService.class);

    private final InstallSettings settings;
    private final DataProvider dataProvider;
    private final AccountService accountService;
    private final RoleService roleService;
    private final RoleMatrixService roleMatrixService;
    private final CategoryService categoryService;
    private final PasswordHashes passwordHashes;
    private final StaticAppDeployer staticAppDeployer;
    private final SiteReady siteReady;
    private final UrlTemplateSeeds urlTemplateSeeds;
    private volatile Boolean existingSite;

    public InstallService(InstallSettings settings, DataProvider dataProvider, AccountService accountService,
            RoleService roleService, RoleMatrixService roleMatrixService, CategoryService categoryService,
            PasswordHashes passwordHashes, StaticAppDeployer staticAppDeployer,
            SiteReady siteReady, UrlTemplateSeeds urlTemplateSeeds) {
        this.settings = settings;
        this.dataProvider = dataProvider;
        this.accountService = accountService;
        this.roleService = roleService;
        this.roleMatrixService = roleMatrixService;
        this.categoryService = categoryService;
        this.passwordHashes = passwordHashes;
        this.staticAppDeployer = staticAppDeployer;
        this.siteReady = siteReady;
        this.urlTemplateSeeds = urlTemplateSeeds;
    }

    public Map<String, Object> status() {
        boolean mongoReady = dataProvider.ping();
        if (mongoReady) {
            noteExistingOwnerIfPresent();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mongoReady", mongoReady);
        body.put("mongoFromEnv", dataProvider.usesEnvironmentCredentials());
        body.put("hasOwner", mongoReady && hasOwner());
        body.put("existingSite", isExistingSite());
        String title = settings.getValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL,
                SettingKeys.KEY_TAG_COMPANY_TITLE);
        body.put("siteTitle", title == null ? "" : title);
        return body;
    }

    public Map<String, Object> connectMongo(Map<String, String> body) {
        if (dataProvider.usesEnvironmentCredentials()) {
            if (!dataProvider.ping()) {
                throw new InstallException(400, "Environment Mongo is set but not reachable");
            }
            siteReady.noteMongoReady();
            noteExistingOwnerIfPresent();
            return Map.of("ok", true, "persistedJson", false, "skipped", true);
        }
        String address = text(body, "address");
        String port = firstNonBlank(text(body, "port"), "27017");
        String dbname = text(body, "dbname");
        String user = text(body, "user");
        String password = body == null ? "" : body.getOrDefault("password", "");
        String type = firstNonBlank(text(body, "type"), "mongodb");
        if (address.isEmpty() || dbname.isEmpty()) {
            throw new InstallException(400, "Address and database name are required");
        }
        dataProvider.useFormCredentials(type, address, port, dbname, user, password);
        if (!dataProvider.ping()) {
            throw new InstallException(400, "Cannot reach Mongo with these credentials");
        }
        siteReady.noteMongoReady();
        noteExistingOwnerIfPresent();
        settings.putValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE, SettingKeys.KEY_DBMS_TYPE, type);
        settings.putValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE, SettingKeys.KEY_DBMS_ADDRESS, address);
        settings.putValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE, SettingKeys.KEY_DBMS_PORT, port);
        settings.putValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_INSTANCE, SettingKeys.KEY_DBMS_DBNAME, dbname);
        settings.putValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS, SettingKeys.KEY_DBMS_ACCESS_USER, user);
        settings.putValue(SettingKeys.CONTEXT_DATASOURCE_DBMS_ACCESS, SettingKeys.KEY_DBMS_ACCESS_PSWD,
                password == null ? "" : password);
        boolean persisted = settings.persistSecretsToDisk();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("persistedJson", persisted);
        if (!persisted) {
            result.put("message", "Connected for this process. Set CSE_MONGO_* (or a writable /var/cse instance dir) so the next boot keeps it.");
        }
        return result;
    }

    public AccountEntity createOwner(Map<String, String> body) {
        if (!dataProvider.ping()) {
            throw new InstallException(400, "Mongo is not ready");
        }
        if (hasOwner()) {
            throw new InstallException(409, "An account already exists");
        }
        noteExistingOwnerIfPresent();
        String login = text(body, "login");
        String email = text(body, "email");
        String password = body == null ? "" : body.getOrDefault("password", "");
        String retype = body == null ? "" : body.getOrDefault("passwordRetype", "");
        if (login.isEmpty() || email.isEmpty()) {
            throw new InstallException(400, "Login and email are required");
        }
        if (password == null || password.isEmpty() || !password.equals(retype)) {
            throw new InstallException(400, "Passwords don't match");
        }
        if (passwordHashes.isTooLong(password)) {
            throw new InstallException(400, "Password is too long");
        }
        AccountEntity entity = new AccountEntity(new AccountData(login, passwordHashes.hash(password),
                email, SecurityLevel.OWNER));
        if (roleService != null) {
            roleService.ensureSeeded();
            if (roleMatrixService != null) {
                roleMatrixService.ensureSeeded(roleService);
            }
            AccountRoles.assignBySlug(entity.getAccountData(), roleService,
                    RoleSeeds.OWNER, true);
        }
        accountService.insert(entity);
        return entity;
    }

    public Map<String, Object> finish(Map<String, String> body, String contextPath) {
        if (!dataProvider.ping()) {
            throw new InstallException(400, "Mongo is not ready");
        }
        if (!hasOwner()) {
            throw new InstallException(400, "Create an owner first");
        }
        String companyTitle = firstNonBlank(text(body, "companyTitle"),
                text(body, SettingKeys.KEY_TAG_COMPANY_TITLE));
        if (isExistingSite()) {
            seedCategory(companyTitle);
        } else {
            if (companyTitle.isEmpty()) {
                throw new InstallException(400, "Site title is required");
            }
            applyPersonal(body, companyTitle);
            seedCategory(companyTitle);
        }
        urlTemplateSeeds.ensureSeeded();
        siteReady.markFinished();
        try {
            staticAppDeployer.undeployEngineApp(StaticAppDeployer.INSTALLER_SLUG);
        } catch (IOException ex) {
            LOGGER.warn("Could not undeploy installer: {}", ex.getMessage());
        }
        String context = contextPath == null ? "" : contextPath;
        return Map.of("ok", true, "redirect", context + "/editor");
    }

    private void applyPersonal(Map<String, String> body, String companyTitle) {
        for (String key : SettingKeys.INSTALL_PERSONAL_KEYS) {
            String value = text(body, key);
            if (SettingKeys.KEY_TAG_COMPANY_TITLE.equals(key) && value.isEmpty()) {
                value = companyTitle;
            }
            settings.putValue(SettingKeys.CONTEXT_DATASOURCE_PERSONAL, key, value);
        }
        settings.persistContext(SettingKeys.CONTEXT_DATASOURCE_PERSONAL);
    }

    private boolean isExistingSite() {
        return Boolean.TRUE.equals(existingSite);
    }

    private void noteExistingOwnerIfPresent() {
        if (existingSite != null) {
            return;
        }
        existingSite = hasOwner();
    }

    private boolean hasOwner() {
        try {
            List<AccountEntity> accounts = accountService.getAllAccounts();
            return accounts != null && !accounts.isEmpty();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void seedCategory(String companyTitle) {
        try {
            if (!categoryService.getAllCategories().isEmpty()) {
                return;
            }
            AccountEntity creator = accountService.getAllAccounts().get(0);
            String title = companyTitle.isEmpty() ? "Home" : companyTitle;
            CategoryData data = new CategoryData("home", title, "", true, true);
            categoryService.insert(new CategoryEntity(data, creator));
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not seed a category: {}", ex.getMessage());
        }
    }

    private static String text(Map<String, String> body, String key) {
        if (body == null || body.get(key) == null) {
            return "";
        }
        return body.get(key).trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
