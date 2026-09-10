package com.ravenherz.cse.constants;

public final class Strings {

    public static final String EXTENSION_PNG = ".png";
    public static final String EXTENSION_HTML = ".html";
    public static final String EXTENSION_JSON = ".json";

    public static final String FILENAME_SETUP0_HTML = "step0.html";
    public static final String FILENAME_SETUP1_HTML = "step1.html";
    public static final String FILENAME_SETUP2_HTML = "step2.html";
    public static final String FILENAME_SETUP3_HTML = "step3.html";
    public static final String FILENAME_SETUP4_HTML = "step4.html";

    public static final String PATH_SOCIAL_NETWORK_DEFINITIONS = "/static/content-public/extensions/social-networks/";
    public static final String PATH_CONFIGURATION = "/static/content-private/configuration/";
    public static final String PATH_SETUP_PAGES = "/static/content-private/page-blocks/html/setup/";
    public static final String PATH_STATIC_PAGES = "/static/content-public/pages/";
    public static final String PATH_STATIC_PAGES_DISK = "/var/cse/content-cache/apps/";
    public static final String PATH_SOCIAL_LOGO_DEFAULT = "static/content-public/extensions/social-networks/default.png";

    public static final String POSTFIX_CONTAINER = "-container";

    public static final String STR_DELIM_SLASH_F = "/";
    public static final String STR_DELIM_COLON = ":";
    public static final String STR_DELIM_COLOND = "::";
    public static final String STR_DELIM_COLONDWWS = " :: ";
    public static final String STR_DELIM_WS = " ";
    public static final String STR_DELIM_HYPHEN = "-";
    public static final String STR_DELIM_CPRIGHTWWS = " © ";
    public static final String STR_DELIM_DOT = ".";
    public static final String STR_DUMMY = "DUMMY";
    public static final String STR_EMPTY = "";

    public static final String STR_PROTOCOL_MAILTO = "mailto:";
    public static final String STR_PROTOCOL_HTTPS = "https://";
    public static final String STR_SPRING = "spring";

    public static final String STR_REPLACE_TYPE = "%%TYPE%%";
    public static final String STR_REPLACE_CLASS = "%%CLASS%%";
    public static final String STR_REPLACE_MSG = "%%MESSAGE%%";
    public static final String STR_REPLACE_TIME_STRING = "%%TIMESTR%%";

    public static final String STR_LOG_EVENT = "event   ";
    public static final String STR_LOG_ERROR = "error   ";
    public static final String STR_LOG_DEBUG = "debug   ";
    public static final String STR_LOG_SUCCESS = "success ";
    public static final String STR_LOG_FAILURE = "failure ";

    public static final String STR_BUILT_BY = "Built by";
    public static final String STR_ADDRESS = "Located at:";
    public static final String STR_UNKNOWN_ERR_NAME = "Unknown";
    public static final String STR_UNKNOWN_ERR_DESCRIPTION = "Totally unexpected error code. Agent Mulder is on a way";
    public static final String STR_PHONE = "Phone:";
    public static final String STR_EMAIL = "E-mail:";
    public static final String STR_CLICK = "click";

    private Strings() {
    }

    public static String jsonKeyToCamelCase(String key) {
        StringBuilder out = new StringBuilder(STR_EMPTY);
        String[] parts = key.split(STR_DELIM_HYPHEN);
        for (String part : parts) {
            if (out.toString().equals(STR_EMPTY)) {
                out = new StringBuilder(part);
            } else {
                out.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
            }
        }
        return out.toString();
    }
}
