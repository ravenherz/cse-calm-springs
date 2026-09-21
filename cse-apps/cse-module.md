# cse-apps

Static app packs and the app-store access checks.

## Does

- Validate and explode `.cseapp` archives, read `version.manifest`, and check app-store table names.

## Does not

- Contain the app projects (`app-admin`, `app-setup`, `app-login`, `app-projects`).
- Serve `/editor/apps` or `/apps/**`. Those controllers stay in the WAR.
