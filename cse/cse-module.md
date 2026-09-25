# cse

The Spring Boot WAR. Tomcat deploy, HTTP, and the admin and public screens that have not moved into a library.

## Does

- Boot entry, MVC config, and Cargo deploy.
- Controllers for the public site. Admin page controllers live in `cse-admin`.
- Public redirect filter: a stored path sends `Location`; `/editor/**` and the code redirects stay on their controllers.
- Admin catalog tree, access checks on requests, and theme HTML tags.
- Packs selected front ends into the WAR: `setup.cseapp`, `admin.cseapp`, `login.cseapp`, and `modern.csetheme`.

## Does not

- Own the persistence port (`cse-db-api`) or the Mongo adapter (`cse-db-mongo`).
- Own codecs, article PDF, embeds, theme-pack install, `.cseapp` explode, or `.csesite` zip logic. Those live in the `cse-*` libraries.
- Copy every packed app and theme into the WAR. `packFrontend` builds them all; only the four packs above are installed.
