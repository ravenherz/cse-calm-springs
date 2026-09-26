# cse-redirect

Operator-defined redirects. One document, one store interface, one in-memory map.

## Does

- Own `ResourceRedirectEntity` and `ResourceRedirectService`. They extend `cse-dal-core`.
- Check a row before save: path shape, reserved prefixes, duplicate source, and loops.
- Load enabled rows into `RedirectIndex`, a `PathResolver`.
- Publish the generic editor form and its catalog presentation through `RedirectAdmin`.
- Store the rows through `RedirectRecords`.

## Does not

- Open Mongo. `cse-db-mongo` implements the store.
- Send HTTP, render HTML, or register a Spring mapping per row.
- Import page, file, account, theme, or app types. A page target is an `EntityId` plus the public path.
