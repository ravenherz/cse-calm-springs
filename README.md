A simple personal website engine (see https://www.ravenherz.com/)

I'll put some more info later, and probably will apply some refactoring. There's lot of not-so-great code written by me when I was a Junior Java Developer. Let's see what I can fix now :)

Product pitch lives in [docs/pitch.md](docs/pitch.md).

# To cut a long story short:

### Plans:
- UI re-design
- Dockerized deployment (maybe?) — sketch in `internal-docs/dockerized-delivery.md`



### 0.5.X (mostly 2026)
Tomcat 10 web-app
- External static-apps support (`.cseapp` → `/static-pages/{slug}/`)
- Admin console UI rework (dedicated editor pages; public in-place modal retired)
- Admin log tail (`/editor/logs`, ADMIN+)
- HEIC/HEIF upload → stored JPEG; optional low-res preview (`image-upload.json`)
- Albums: item type that shows a resource group as a gallery (`/?album=`)
- Image descriptions (upload + edit); used as album captions / alt text
- Modern theme: tighter type/spacing; color schemas `studio`, `atelier`, `linen`, `midnight`, `chalk`
- JS public theme (`theme-js-client`, id `client`, `shell: js`): no Thymeleaf; renders `GET /rest/site` (documented on `/editor/api`)
- Hall public theme (`theme-js-hall`, id `hall`, `shell: js`): WebGL exhibition of the same snapshot
- Tag listings: tagged items → actor access → active+readable categories
- Auth: Argon2id on the server, HttpOnly/Secure/SameSite cookies, session kill on logout, SecureRandom tokens, login rate limit (`internal-docs/auth-flow.md`)
- Spring Security: CSRF (`XSRF-TOKEN` cookie + `_csrf` on forms), cookie session in `SecurityContext` (no `HttpSession`/`formLogin`); `/editor` gated by the filter (`ROLE_ADMIN` for apps/logs)
- Headers: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, HSTS on HTTPS; CSP (inline handlers + Quill + Google Fonts; `/static-pages` excluded)
- Engine rename in code/data: package `com.ravenherz.cse`, Mongo collections `cse-*` (public URL still `/rhz-we`)
- Mongo: Morphia → Spring Data (`MongoTemplate`, ObjectId refs, no `_class`); convert dumps with `scripts/migrate-morphia-to-spring-data.js`
- First-boot installer (not the old in-theme dialogue):
  - Packed app `app-setup/` → classpath `install/setup.cseapp` → `/static-pages/setup/` while unconfigured; `GET /` 302s there; gone after finish
  - `GET/POST /install/**` (status, mongo, owner, finish) until the site is ready, then 404
  - Mongo: Connect form, or skip with `CSE_MONGO_*` / `CSE_MONGODB_URI`; optional mongosh guide if you only have a cluster admin (app user must live on the **app database**, `authSource` is not `admin`)
  - Passwords: env or `/var/cse/{tomcat-context}/content-private/configuration/secret-dbms-*.json` — never editor settings, never the WAR
  - Ready is derived (Mongo binding + owner / `.site-ready`), not an `is-configured` flag in JSON
  - Marker + reachable Mongo with **no accounts** drops `.site-ready` and reopens the installer (failed install / empty import). A Mongo blip does not.
  - `CSE_FORCE_SETUP=true` (or `cse.force.setup`) reopens setup even when accounts exist. Unset it after Finish, or the next boot opens setup again.
  - New site: last step is public copy (title, contact, copyright, welcome). Existing database keeps its Mongo settings
  - Admin API tab (`/editor/api`): public JSON (`/account`, `/rest`), site URLs, first-boot `/install`
  - Disk: `/var/cse` is the machine (certs); each WAR uses `/var/cse/{context}/` for cache, secrets, and the installer explode. History: `internal-docs/setup-wizzard-progress.md`
- Persistence Gradle split: `cse-db-api` (ports + entities), `cse-db-mongo` (current adapter, on the WAR), `cse-db-postgres` (stub, not wired)

### 0.4.X (mostly 2026)
Tomcat 10 web-app
- Admin console, resource management via UI
- In-place page edit modal
- HTML, MD rendering for page contents
- Audio-player (and playlists)

### 0.3.X (mostly 2025)
Tomcat 10 web-app
Technical migration to actualize codebase
- Spring Boot 3.5

### 0.2.X (mostly 2018, put on hold until 2025)
Tomcat 8 web-app
- MongoDB instead of RDBMS (Morphia ORM)
- Spring Framework instead of Java EE
- Thymeleaf as template engine
- Login panel, comments section
- Deployment with gradle scripts

### 0.1.X (mostly 2017)
Tomcat 7 web-app
- Some RDBMS
- Java EE Servlets
- JSP
