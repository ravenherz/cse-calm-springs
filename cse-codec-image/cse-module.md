# cse-codec-image

Codec for still images and resource thumbnails. A codec transforms bytes. It does not own entities, DAOs, or HTTP.

## Does

- JPEG and HEIC conversion, image metadata, account avatars, and PDF-resource thumbnails.
- Read the HEIF container (tile grid, decoder config, rotation) and hand the coded tiles to FFmpeg.

## Does not

- Render an article as PDF. That is `cse-codec-pdf`.
- Probe or transcode video, or ship FFmpeg. That is `cse-codec-video`, which this module calls
  for HEVC decoding because a HEIC photo is HEVC frames in an ISOBMFF container.
- Own a DAL. Resource documents stay with `cse-media`.
