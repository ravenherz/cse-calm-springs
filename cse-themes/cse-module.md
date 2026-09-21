# cse-themes

Install and resolve public theme packs.

## Does

- Read `.csetheme` manifests, catalog installed themes, and resolve templates from disk.

## Does not

- Contain the theme projects (`theme-thymeleaf-*`, `theme-js-client`).
- Serve `/editor/themes`. `ThemesController` stays in the WAR.
