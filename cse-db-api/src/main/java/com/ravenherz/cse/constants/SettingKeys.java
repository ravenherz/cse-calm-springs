package com.ravenherz.cse.constants;

public final class SettingKeys {

    public static final String CONTEXT_DATASOURCE_PERSONAL = "config-personal";
    public static final String CONTEXT_DATASOURCE_BUILD_INFO = "config-build-info";
    public static final String CONTEXT_DATASOURCE_DBMS_INSTANCE = "secret-dbms-instance";
    public static final String CONTEXT_DATASOURCE_DBMS_ACCESS = "secret-dbms-access";
    public static final String SECRET_PREFIX = "secret-";
    public static final String CONTEXT_DATASOURCE_VIEW = "config-view";
    public static final String CONTEXT_DATASOURCE_SETUP = "config-setup";
    public static final String CONTEXT_DATASOURCE_CORE = "config-core";
    public static final String CONTEXT_DATASOURCE_IMAGE_UPLOAD = "image-upload";

    public static final String KEY_TAG_WELCOME_TITLE = "welcome-title";
    public static final String KEY_TAG_WELCOME_MESSAGE = "welcome-message";
    public static final String KEY_TAG_WELCOME_DESCRIPTION = "welcome-description";
    public static final String KEY_TAG_BUILDER_LINK_TITLE = "builder-title";
    public static final String KEY_TAG_BUILDER_LINK_REFER = "builder-refer";
    public static final String KEY_TAG_COPYRIGHT_COMMENT = "copyright-comment";
    public static final String KEY_TAG_SOFT_VERSION_PRODUCT = "version-product";
    public static final String KEY_TAG_SOFT_VERSION_BRANCH = "version-branch";
    public static final String KEY_TAG_SOFT_VERSION_VERSION = "version-version";
    public static final String KEY_TAG_COPYRIGHT_HOLDER = "copyright-holder";
    public static final String KEY_TAG_COPYRIGHT_SINCE = "copyright-since";
    public static final String KEY_TAG_COMPANY_ADDRESS = "company-address";
    public static final String KEY_TAG_COMPANY_PHONE = "company-phone";
    public static final String KEY_TAG_COMPANY_EMAIL = "company-email";
    public static final String KEY_TAG_COMPANY_SOCIAL = "company-social";
    public static final String KEY_TAG_COMPANY_TITLE = "company-title";
    public static final String KEY_DBMS_ADDRESS = "dbms-instance-address";
    public static final String KEY_DBMS_PORT = "dbms-instance-port";
    public static final String KEY_DBMS_TYPE = "dbms-instance-type";
    public static final String KEY_DBMS_DBNAME = "dbms-instance-dbname";
    public static final String KEY_DBMS_ACCESS_USER = "dbms-access-user";
    public static final String KEY_DBMS_ACCESS_PSWD = "dbms-access-pswd";
    public static final String KEY_LOGIN_IN_SUMMARY = "login-panel-in-summary-block";
    public static final String KEY_NAV_FOOTER_NUM_OF_COLS = "navigation-footer-num-of-cols";
    public static final String KEY_VIEW_VISUAL_THEME = "visual-theme";
    public static final String KEY_STYLES_THEME = "styles-theme";
    public static final String KEY_STYLES_SCHEMA = "styles-schema";
    public static final String KEY_DEFAULT_PAGE = "default-page";
    public static final String KEY_PREVIEW_MAX_WIDTH = "preview-max-width";
    public static final String KEY_QUALITY_FACTOR = "quality-factor";

    /**
     * Public {@code config-personal} keys the greenfield installer may set.
     * Blank values hide the matching public tags.
     */
    public static final String[] INSTALL_PERSONAL_KEYS = {
            KEY_TAG_COMPANY_TITLE,
            KEY_TAG_COMPANY_EMAIL,
            KEY_TAG_COMPANY_PHONE,
            KEY_TAG_COMPANY_ADDRESS,
            KEY_TAG_COMPANY_SOCIAL,
            KEY_TAG_COPYRIGHT_HOLDER,
            KEY_TAG_COPYRIGHT_SINCE,
            KEY_TAG_COPYRIGHT_COMMENT,
            KEY_TAG_WELCOME_TITLE,
            KEY_TAG_WELCOME_MESSAGE,
            KEY_TAG_WELCOME_DESCRIPTION,
            KEY_TAG_BUILDER_LINK_TITLE,
            KEY_TAG_BUILDER_LINK_REFER
    };

    private SettingKeys() {
    }

    public static boolean isSecretContext(String context) {
        return context != null && context.startsWith(SECRET_PREFIX);
    }
}
