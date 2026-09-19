# URL Templates

A URL template is a reusable link pattern — YouTube, Telegram, and the rest — with an icon, default copy, and a `%s` hole for the public id. Define it once. Embed it on any page.

## Open URL Templates

**Content → URL Templates**. Tiles are tagged **URL**. **+** opens **New URL template**.

## New URL template

| Field | What it does |
| --- | --- |
| **Template id** | Lowercase letters, numbers, and hyphens. This is `templateId` in the embed tag. |
| **URL pattern** | The address, with `%s` where the public id goes. Example: `https://youtube.com/%s`. |
| **Default text** | Shown on the public page unless the tag sets `textOverride`. `%s` is replaced by the tag’s `id`. |
| **Image** | Embedded image, 256×256 px max. PNG keeps transparency; larger uploads are scaled down. |

**Create URL template** saves the pattern.

## Embed

In a [page](pages.md) body:

```html
<cse-url templateId="youtube" id="ravenherz" size="m" textOverride="My override text" secondLine="Displayed second line" overrideUrl="https://bandcamp.com/album/tracks"></cse-url>
```

| Attribute | Required | What it does |
| --- | --- | --- |
| **templateId** | yes | Looks up the URL template. |
| **id** | yes | Substituted into the pattern and default text. |
| **size** | no | `xs` a normal link; `s` a small icon (footer-like); `m` a 64×64 card; `l` the same card at full width. Default is `m`. |
| **textOverride** | no | Replaces default text. Rendered as the bold line. |
| **secondLine** | no | Extra line under the title on `m` and `l`. |
| **overrideUrl** | no | Replaces the href. Icon and text still come from the template. Use this for a Bandcamp album or track instead of the artist pattern. |

Wrap several tags to share a size:

```html
<cse-urls sizeOverride="s">
  <cse-url templateId="youtube" id="ravenherz"></cse-url>
  <cse-url templateId="telegram" id="ravenherz"></cse-url>
</cse-urls>
```

You can also drag a template from the Catalog tree into the page textarea. Fill in `id` after the drop.

The public footer uses the same small (`s`) icons: Settings **company-social** is `templateId:handle` pairs, rendered as `<cse-urls sizeOverride="s">`.

## Edit and delete

Click the tile, or right-click → **Edit**. **×** removes the template, not the pages that embed it.
