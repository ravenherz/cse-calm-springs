# cse-media

Files and folders: the media documents, and the disk cache that serves them.

## Does

- Own resource, resource-group, and data-chunk documents and their store interfaces.
- Writable disk root, upload size limits, and the protected-file cache.

## Does not

- Decode JPEG, HEIC, MP3, or video. Those are `cse-codec-*`.
- Own pages, accounts, themes, or apps.
- Open Mongo. `cse-db-mongo` implements the stores.
