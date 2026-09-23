# cse-content

Article text and the content documents: pages, albums, categories, playlists, and URL templates.

## Does

- Own those documents and their store interfaces. They extend `cse-dal-core`.
- A page image, playlist cover, playlist track, and album resource group are an `EntityId`.
- Render markdown and expand `<cse-*>` embeds.
- Build `PageEvent` for the public index.

## Does not

- Own files, accounts, themes, or apps.
- Open Mongo. `cse-db-mongo` implements the stores.
- Render theme chrome or serve `/`.
