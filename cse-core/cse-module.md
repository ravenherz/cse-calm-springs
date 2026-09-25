# cse-core

Shared types that more than one library would otherwise copy.

## Does

- `SiteConfigured`: whether first-run setup has finished. The WAR implements it (`SiteReady`).
- `PathResolver` and `PathTarget`: a context-relative path may map to a same-host target. The WAR asks every resolver and sends the HTTP redirect.
- `ReservedPaths`: prefixes the public redirect lookup and redirect saves both honor.
- `AdminSectionSource`, `AdminSection`, `AdminField`, `AdminPresentation`, and `AdminSectionRecords`: a feature module's editor form, catalog glyph, and stored rows. `cse-admin` collects these and does not import the feature entity.

## Does not

- Start Spring, open Mongo, or encode media.
- Decide which request wins, send a redirect, or render a page.
- Own redirect documents or admin controllers.
