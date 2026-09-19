# Pages

A page is a story with a stable address — title, cover, tags, and a body in Markdown or HTML. Pages live under a [category](categories.md) in **Content → Categories**.

![Pages in the music category](pages-md-image-1.jpg)

## Open a category’s pages

In the tree, expand **Content → Categories** and click the category. The pane lists page tiles tagged **PAGE** (and **ALBUM** for galleries). Each tile shows a cover when the page has a featured image, plus the URL name.

**+** opens **New page** with that category already selected. Right-click the category → **New page** does the same.

## New page

![New page form](pages-md-image-2.jpg)

| Field | What it does |
| --- | --- |
| **Page ID (URL name)** | The public path. Required. Use lowercase letters, numbers, and hyphens. |
| **Category** | Homepage section. You can leave it empty, but the page then stays off the section lists. |
| **Title** | Main title. |
| **Header** / **Sub Header** | Lines the theme may show above or beside the body. |
| **Tags** | Comma-separated. |
| **Content (Markdown/HTML)** | The body. Markdown for speed; HTML when you need control. |

**Create page** writes the page and returns to Catalog.

The body can leave custom tags. They stay as tags in Mongo and expand when the public page renders. Drag a page, category, app, playlist, or image from the Catalog tree into the textarea, or paste:

```html
<cse-playlist id="your-playlist-id"></cse-playlist>
<cse-url templateId="youtube" id="channel-id"></cse-url>
<cse-image id="resource-object-id"></cse-image>
<cse-binary id="resource-object-id" width="400px" height="300px" textOverride="CV"></cse-binary>
<cse-binary id="resource-object-id" size="m" textOverride="CV"></cse-binary>
<cse-page id="page-or-album-url-name"></cse-page>
<cse-page id="page-or-album-url-name" size="m"></cse-page>
<cse-category id="category-name"></cse-category>
<cse-app id="app-slug"></cse-app>
<cv-card imageId="resource-object-id" imageRectangle="256px" company="Zvuk" role="Senior Big Data Engineer" interval="2022-05-01;2025-03-31" location="Moscow"></cv-card>
<cse-md paddingLeft="250px">
Worked on streaming pipelines and warehouse jobs.
</cse-md>
```

`cse-image` is a full-width picture (`id` is the resource ObjectId, or the public path). `<cse-binary>` uses the same card as `cse-page`: first-page preview on the left, optional `textOverride` on the right, and a link that opens the PDF. The preview keeps the page aspect ratio. `width` / `height` are max sizes (`400px`; height may be omitted). `<cse-binary size="m">` is a compact row: preview fitted in 32×32 and a title (`textOverride`, or the filename). The other cards share that left-preview layout. Albums use `cse-page` and link to `/?album=`. `<cse-page size="m">` is a compact row: 32×32 image and title only. Default `size` is `l`.

`<cv-card>` is a résumé row: square logo (`imageId`, `imageRectangle` such as `256px`), company as an `h3` (same as Markdown `###`), role in bold, then the employment line. `interval` is `yyyy-MM-dd;Now` for current work (`September 2025 – Present (1 year 1 month)`) or `yyyy-MM-dd;yyyy-MM-dd` for a finished role (`September 2025 – October (1 month)`). `location` is optional.

`<cse-md>` renders its inner text as Markdown (same as the page body). `paddingLeft` is optional, default `0px` — use `250px` to line a block up under a 256px CV logo.

See [Playlists](playlists.md), [URL Templates](url-templates.md), [Categories](categories.md), and [Apps](apps.md).

## Edit

Click the tile, or right-click → **Edit**. The URL name does not change on this form; **Rename** in the tree changes the title shown in Catalog.

![Edit page](pages-md-image-3.jpg)

Editing adds **Featured Image**: pick an uploaded picture, or none. The rest of the fields match create. **Save** updates the live page.

## Delete

**×** on the tile or **Delete** in the menu removes the page. Confirmation first. The featured image file stays in its media folder.
