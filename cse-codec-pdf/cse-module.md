# cse-codec-pdf

Codec for public article PDF. A codec transforms bytes. It does not own entities, DAOs, or HTTP.

## Does

- Turn a rendered article into a PDF (Jsoup + Flying Saucer).

## Does not

- Thumbnail PDF resources. That is `cse-codec-image`.
- Own the export HTTP call. The WAR loads media and invokes this library.
- Own a DAL.
