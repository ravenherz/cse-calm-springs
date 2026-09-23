# cse-codec-video

Codec for FFmpeg probe and transcode (JavaCPP). A codec transforms bytes. It does not own entities, DAOs, or HTTP.

## Does

- Run FFmpeg to inspect a video and transcode it.
- Own the bundled FFmpeg binaries and the process runner. `cse-codec-image` uses them to
  decode HEVC stills out of HEIC.

## Does not

- Queue uploads or build JPEG posters. Those stay in the WAR.
- Store the source file. That is Mongo plus `cse-media`.
- Own a DAL.
