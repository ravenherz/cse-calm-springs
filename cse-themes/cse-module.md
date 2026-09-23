# cse-themes

Theme documents and theme-pack install.

## Does

- Own theme documents and `ThemeService`.
- Read `.csetheme` manifests, catalog installed themes, and resolve templates from disk.

## Does not

- Contain the theme projects (`theme-thymeleaf-*`, `theme-js-client`).
- Serve `/editor/themes`. `ThemesController` stays in the WAR.
- Open Mongo. `cse-db-mongo` implements the store.
