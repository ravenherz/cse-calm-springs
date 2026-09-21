# cse

The Spring Boot WAR. Tomcat deploy, HTTP, and the admin and public screens that have not moved into a library.

## Does

- Boot entry, MVC config, and Cargo deploy.
- Controllers for the public site, Catalog, accounts, settings, logs, and JSON auth.
- Admin catalog tree, access checks on requests, and theme HTML tags.
- Packs selected front ends into the WAR: `setup.cseapp`, `admin.cseapp`, `login.cseapp`, and `modern.csetheme`.

## Does not

- Own the persistence port (`cse-db-api`) or the Mongo adapter (`cse-db-mongo`).
- Own codecs, article PDF, embeds, theme-pack install, `.cseapp` explode, or `.csesite` zip logic. Those live in the `cse-*` libraries.
- Copy every packed app and theme into the WAR. `packFrontend` builds them all; only the four packs above are installed.
