# cse-content

Article text: markdown, embeds, and the public page view model.

## Does

- Render markdown and expand `<cse-*>` embeds (page, album, image, video, playlist, category, app, URL).
- Build `PageEvent` for the public index (header is the display title; pages and albums have no stored title).

## Does not

- Render theme chrome or body tags. Those stay in the WAR.
- Store items or serve `/`.
