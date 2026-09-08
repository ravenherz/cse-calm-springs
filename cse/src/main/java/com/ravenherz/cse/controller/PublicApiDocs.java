package com.ravenherz.cse.controller;

import java.util.List;

/**
 * Curated public HTTP surface for the admin API tab. Editor HTML forms are not listed.
 */
public final class PublicApiDocs {

    private PublicApiDocs() {
    }

    public record Section(String id, String title, String lead, List<Endpoint> endpoints) {
    }

    public record Endpoint(
            String method,
            String path,
            String access,
            boolean csrf,
            String summary,
            String request,
            String response) {
    }

    public static List<Section> sections() {
        return List.of(account(), rest(), site(), install());
    }

    public static List<String> paths() {
        return sections().stream()
                .flatMap(section -> section.endpoints().stream())
                .map(Endpoint::path)
                .toList();
    }

    private static Section account() {
        return new Section("account", "Account",
                "JSON helpers used by the public login panel and JS themes. HTTP status for /account "
                        + "and most /rest POSTs is 200 even when the payload status is 400/429/500 — "
                        + "read status in the body. GET /rest/site uses a real HTTP status.",
                List.of(
                        new Endpoint("POST", "/account/auth", "Public", true,
                                "Sign in. Sets HttpOnly cookies auth-login and auth-session "
                                        + "(SameSite=Lax, 30 days). Rate-limited per client IP.",
                                """
                                {
                                  "loginpanel-username": "owner",
                                  "loginpanel-password": "secret"
                                }""",
                                """
                                { "status": 200, "restObject": null, "message": null }

                                { "status": 400, "message": "Bad credentials" }
                                { "status": 429, "message": "Too many login attempts. Try again later." }"""),
                        new Endpoint("POST", "/account/logout", "Session", true,
                                "Marks the current auth-session finished and expires auth cookies.",
                                null,
                                """
                                { "status": 200 }"""),
                        new Endpoint("POST", "/account/register", "Public", true,
                                "Create a member. Keys are the Param enum names, not the login-panel ids. "
                                        + "ACCOUNT_SHOWN_NAME may be omitted or blank.",
                                """
                                {
                                  "ACCOUNT_LOGIN": "ada",
                                  "ACCOUNT_PASSWORD": "secret",
                                  "ACCOUNT_PASSWORD_RETYPE": "secret",
                                  "ACCOUNT_EMAIL": "ada@example.com",
                                  "ACCOUNT_SHOWN_NAME": "Ada"
                                }""",
                                """
                                { "status": 200, "message": "all done" }

                                { "status": 400, "message": "Login is already taken" }"""),
                        new Endpoint("GET", "/account/activate", "Public", false,
                                "Activate with the token stored at registration. Redirects to /?error=200 "
                                        + "or /?error=400 (HTML, not JSON).",
                                "login, token  (query)",
                                "302  /?error=200")
                ));
    }

    private static Section rest() {
        return new Section("rest", "REST helpers",
                "Theme and login-panel scripts POST JSON here. Success uses the RestResponse envelope "
                        + "(status, restObject, message) except /rest/error and GET /rest/site.",
                List.of(
                        new Endpoint("GET", "/rest/site", "Public", false,
                                "JSON snapshot for JS themes. Same query as GET / (page, album, tag, category, error). "
                                        + "ACL-aware. Missing page/album is HTTP 404 with this body; ?error= stays HTTP 200. "
                                        + "Not a RestResponse envelope. JS shells render this body.",
                                "?page={uri}  &album={uri}  &tag=  &category=  &error=",
                                """
                                {
                                  "configured": true,
                                  "contextPath": "/rhz-we",
                                  "stylesTheme": "modern",
                                  "stylesSchema": "linen",
                                  "stylesShell": "modern",
                                  "siteName": "Calm Springs",
                                  "authenticated": false,
                                  "username": null,
                                  "view": "home",
                                  "htmlTitle": "Calm Springs",
                                  "nav": [{ "slug": "pages", "title": "Pages", "itemName": "pages" }],
                                  "sections": [{
                                    "slug": "pages",
                                    "title": "Pages",
                                    "pages": [{
                                      "uri": "hello",
                                      "title": "Hello",
                                      "href": "./?page=hello",
                                      "album": false
                                    }]
                                  }],
                                  "introPage": null,
                                  "page": null,
                                  "error": null
                                }"""),
                        new Endpoint("POST", "/rest/forms/render", "Public", true,
                                "Render a configured form id to HTML (cse-tiny.js). restObject is the markup.",
                                """
                                { "form": "form-id" }""",
                                """
                                { "status": 200, "restObject": "<div id='form-id'>...</div>" }"""),
                        new Endpoint("POST", "/rest/markdown/render", "Public", true,
                                "Markdown to HTML, then expand <cse-playlist id=\"...\"/> embeds.",
                                """
                                { "markdown": "# Hello" }""",
                                """
                                { "status": 200, "restObject": "<h1>Hello</h1>" }"""),
                        new Endpoint("POST", "/rest/error", "Public", true,
                                "Copy for /?error= codes (cse-error.js). Body is not a RestResponse envelope. "
                                        + "Unknown codes fall back to 500.",
                                """
                                { "error": "401" }""",
                                """
                                {
                                  "code": 401,
                                  "name": "Unauthorized",
                                  "description": "Authentication is required..."
                                }""")
                ));
    }

    private static Section site() {
        return new Section("site", "Public site",
                "HTML, media, and GET /rest/site for JS shells. /content-public/** is static files from the WAR "
                        + "plus exploded theme packs under /content-public/themes/{id}/.",
                List.of(
                        new Endpoint("GET", "/", "Public", false,
                                "Theme shell. Unconfigured instances redirect to /static-pages/setup/. "
                                        + "Query: page, album, tag, category, error.",
                                "?page={uri}  &album={uri}  &tag=  &category=  &error=",
                                "text/html"),
                        new Endpoint("GET", "/content-protected/**", "ACL", false,
                                "Media by public path. Read access is checked; otherwise redirect to "
                                        + "/content-public/cse-core/images/no-image.jpg.",
                                "/content-protected{pathPublic}",
                                "bytes, or 302 to no-image.jpg"),
                        new Endpoint("GET", "/static-pages/{slug}/**", "Public", false,
                                "Exploded .cseapp files. The setup slug 404s after the site is ready.",
                                "/static-pages/admin/",
                                "file bytes")
                ));
    }

    private static Section install() {
        return new Section("install", "First-boot install",
                "JSON while the installer is open. After Finish these routes 404. "
                        + "Unlike /account and /rest, HTTP status is real (404, 400, 409, 429).",
                List.of(
                        new Endpoint("GET", "/install/status", "Setup", false,
                                "Wizard state. A GET also plants the XSRF-TOKEN cookie.",
                                null,
                                """
                                {
                                  "mongoReady": true,
                                  "mongoFromEnv": false,
                                  "hasOwner": false,
                                  "existingSite": false,
                                  "siteTitle": ""
                                }"""),
                        new Endpoint("POST", "/install/mongo", "Setup", true,
                                "Bind Mongo from the form, or skip when CSE_MONGO_* / CSE_MONGODB_URI is set.",
                                """
                                {
                                  "address": "127.0.0.1",
                                  "port": "27017",
                                  "dbname": "cse-site",
                                  "user": "app",
                                  "password": "secret",
                                  "type": "mongodb"
                                }""",
                                """
                                { "ok": true, "persistedJson": true }"""),
                        new Endpoint("POST", "/install/owner", "Setup", true,
                                "Create the OWNER account and set auth cookies. 409 if an account already exists.",
                                """
                                {
                                  "login": "owner",
                                  "email": "owner@example.com",
                                  "password": "secret",
                                  "passwordRetype": "secret"
                                }""",
                                """
                                { "ok": true }"""),
                        new Endpoint("POST", "/install/finish", "Setup", true,
                                "Write public copy, mark the site ready, undeploy setup. "
                                        + "companyTitle aliases company-title. Other config-personal keys "
                                        + "(welcome-title, copyright-holder, …) are optional.",
                                """
                                {
                                  "companyTitle": "Calm Springs",
                                  "company-email": "hi@example.com"
                                }""",
                                """
                                { "ok": true, "redirect": "/rhz-we/editor" }""")
                ));
    }
}
