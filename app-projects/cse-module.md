# app-projects

Optional static app. `packFrontend` builds a `.cseapp`; the WAR does not install it.

## Does

- Pack `src/` into `dist/` as a Catalog app archive.

## Does not

- Get copied into the WAR by `processResources`. An operator uploads the archive.
- Contain CMS controllers.
