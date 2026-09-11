A simple personal website engine (see https://www.ravenherz.com/)

I'll put some more info later, and probably will apply some refactoring. There's lot of not-so-great code written by me when I was a Junior Java Developer. Let's see what I can fix now :)

Product pitch lives in [docs/pitch.md](docs/pitch.md).

# To cut a long story short:

### Plans:
- Dockerized deployment

### 0.7.X
#### Admin-wise rework
- Login panel .cseapp (app-login subproject)
- Small issue tracker .cseapp (app-projects subproject)
- Admin console tree (huge admin UI rework)

### 0.6.X
#### Extensibility update
- External static-apps deployment support (*.cseapp format), implementations:
  - First-boot installer standalone .cseapp (app-setup subproject)
  - Admin console as standalone .cseapp (app-admin subproject)
- External themes support (*.csetheme format)
- Export whole site data as single file (*.csesite)

### 0.5.X (mostly 2026)
#### Security update
- Auth: Argon2id on the server, HttpOnly/Secure/SameSite cookies, session kill on logout, SecureRandom tokens, login rate limit
- Spring Security: CSRF, HSTS on HTTPS; CSP
- Engine rename in code/data: package `com.ravenherz.cse`
- Spring Data replaces Morphia

### 0.4.X (mostly 2025)
#### New features scratch
- Admin console, resource management via UI
- In-place page edit modal (deprecated in 0.6.0)
- HTML, MD rendering for page contents
- Audio-player (and playlists)

### 0.4.X (mostly 2025)
#### Tomcat 10 migration, spring boot
Technical migration to actualize codebase
- Spring Boot 3.5

### 0.2.X (mostly 2018, put on hold until 2025)
#### Tomcat 8 web-app
- MongoDB instead of RDBMS (Morphia ORM)
- Spring Framework instead of Java EE
- Thymeleaf as template engine
- Login panel, comments section
- Deployment with gradle scripts

### 0.1.X (mostly 2017)
#### Tomcat 7 web-app
- Oracle XE RDBMS
- Java EE Servlets
- JSPs
