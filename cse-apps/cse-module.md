# cse-apps

App documents, static app packs, and app-store checks.

## Does

- Own app documents, `AppService`, and app-store table rules.
- Validate and explode `.cseapp` archives and read `version.manifest`.

## Does not

- Contain the app projects (`app-admin`, `app-setup`, `app-login`, `app-projects`).
- Serve `/editor/apps` or `/apps/**`. Those controllers stay in the WAR.
- Open Mongo. `cse-db-mongo` implements the store.
