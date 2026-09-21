# app-admin

Catalog desk UI. Packed as `admin.cseapp` and copied into the WAR.

## Does

- Admin HTML, fragments, and `editor.css` for Catalog, accounts, settings, and the other top-bar tools.

## Does not

- Implement editor HTTP. Controllers stay in the `cse` WAR.
- Ship as a public app. It is the operator UI, exploded from the pack at runtime.
