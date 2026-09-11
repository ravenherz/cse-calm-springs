# Apps

Apps are self-contained pieces that sit beside the rest of the site — experiments, tools, extras — without a second hosting story.

![Content / Apps tiles](apps-md-image-1.jpg)

## Open Apps

**Content → Apps**. Each tile is a product: logo when the pack ships one, otherwise the name.

Tags:

- **WAR** — bundled with the engine (admin, setup, login). You do not remove these.
- **APP** — an installed `.cseapp` pack.
- **STORE** — the operator has enabled the JSON data store for that pack.
- Size and company, when the pack declares them.

**i** shows id, public URL, and file name. The link icon opens the public URL in a new tab. Installed packs that declare a store (`store.json` / `store.tables`) also have **Allow data store**, **Open schema**, and size quotas on the details panel. Those choices live on the app document and survive a re-upload of the same slug. Upload does not turn the store on.

## Install a pack

The gold **+** accepts a `.cseapp` file. Click or drop. The install prefix is the pack’s **`slug`** in `version.manifest` (not the zip filename). After install, the new tile appears in this pane (and in the tree) at `/apps/{slug}/`. If the pack includes a product logo, the tile uses it.

A pack without a slug in `version.manifest` is rejected. Re-uploading a zip with the same slug replaces that install.

## Bundled vs installed

**Calm Springs admin**, **Calm Springs setup**, and **Sign in** are the desk, the first-boot installer, and the public account app. Leave them. Installed packs can be removed with **×** when the tile allows it — confirmation first. Removing a pack keeps its data-store collections so a re-upload of the same slug can restore them.

Apps do not replace [themes](themes.md). A theme is the public skin; an app is an extra product on the same instance.

## Embed

In a [page](pages.md) body:

```html
<cse-app id="your-slug"></cse-app>
```

The id is the pack slug. The public site shows a card (logo, name, description, version) that links to `/apps/{slug}/`. Drag the app from **Content → Apps** in the Catalog tree into the page textarea.

## Data store

A pack may declare tables in `version.manifest` (`store.tables`). The engine stores JSON under `{slug}-{table}` and serves it at `/app-data/{slug}/{table}` (same origin, CSRF on writes). Packed apps can load `/content-public/js/cse-app-data.js`. See the [API](api.md) **App data** tab.

`app-projects` is an example issue tracker (slug `projects`). Gradle packs it with the other `app-*` modules, but it is **not** copied into the WAR. Upload `app-projects/dist/projects-*.cseapp` from Catalog, then **Allow data store**.
