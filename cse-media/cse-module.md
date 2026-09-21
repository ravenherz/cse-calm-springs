# cse-media

Instance disk and the protected-file cache. No codecs.

## Does

- Writable disk root, upload size limits, and servlet file helpers used when a protected file is materialized.

## Does not

- Decode JPEG, HEIC, MP3, or video. Those are `cse-image`, `cse-audio`, and `cse-video`.
- Decide who may read a file. The WAR controller does the access check, then uses this library to cache it.
