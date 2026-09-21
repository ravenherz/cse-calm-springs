# theme-thymeleaf-2000s

Alternate public skin. `packFrontend` builds a `.csetheme`; the WAR does not install it.

## Does

- Thymeleaf pages and the 2000s schemas (`snowy`, `electricity-bright`, `electricity-ravenherz`, `electricity-solar`).

## Does not

- Get copied into the WAR by `processResources`. Install the pack from Catalog.
- Replace `modern` as the fallback when the active theme id is unknown.
