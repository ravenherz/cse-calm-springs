# Playlists

A playlist is music that belongs on the site. Build it once. Embed it in any page. The player is part of the house, not a widget rented from somewhere else.

![Content / Playlists tiles](playlists-md-image-1.jpg)

## Open Playlists

**Content → Playlists**. Tiles are tagged **PLAYLIST**. **+** opens **New playlist**.

Upload MP3s in [Files](files.md) first. Then drag them from the Catalog tree onto the track list.

## New playlist

![New playlist editor](playlists-md-image-2.jpg)

| Field | What it does |
| --- | --- |
| **Playlist id** | Lowercase letters, numbers, and hyphens. This is what you embed. |
| **Title** | Name on the public site. |
| **Description** | Optional copy. |

**Tracks** is playback order. Drag MP3s from the tree onto the list. Drop a folder to add every MP3 in it. Drag tracks in the list to reorder. **Remove** drops a track from this list only — the MP3 stays in Catalog. Track rows show the recording’s cover when one was extracted.

**Cover** is the playlist image. Drag a picture from the tree onto that square.

**Title override** and **Artist override** apply only in this playlist. File tags remain on the recording.

**Create playlist** saves the list.

## Embed

In a [page](pages.md) body:

```html
<cse-playlist id="your-id"></cse-playlist>
<cse-playlist id="your-id" withImage="true"></cse-playlist>
```

`withImage` defaults to false. Set it true to show the playlist cover beside the track list.

Use the same id you entered on the form. You can also drag the playlist from the Catalog tree into the page textarea.

## Edit and delete

Open a playlist by clicking its tile (that opens the same editor as create). **×** removes the playlist, not the audio files.
