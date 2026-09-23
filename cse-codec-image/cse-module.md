# cse-codec-image

Codec for still images and resource thumbnails. A codec transforms bytes. It does not own entities, DAOs, or HTTP.

## Does

- JPEG and HEIC conversion, image metadata, account avatars, and PDF-resource thumbnails.

## Does not

- Render an article as PDF. That is `cse-codec-pdf`.
- Probe or transcode video. That is `cse-codec-video`.
- Own a DAL. Resource documents stay with `cse-media`.
