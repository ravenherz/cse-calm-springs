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

To embed a [playlist](playlists.md) in the body, use:

```html
<cse-playlist id="your-playlist-id"></cse-playlist>
```

## Edit

Click the tile, or right-click → **Edit**. The URL name does not change on this form; **Rename** in the tree changes the title shown in Catalog.

![Edit page](pages-md-image-3.jpg)

Editing adds **Featured Image**: pick an uploaded picture, or none. The rest of the fields match create. **Save** updates the live page.

## Delete

**×** on the tile or **Delete** in the menu removes the page. Confirmation first. The featured image file stays in its media folder.
