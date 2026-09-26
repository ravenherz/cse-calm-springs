# cse-codec-video

Codec for FFmpeg probe and transcode (JavaCPP). It also owns the transcode document. Mongo and the editor page stay outside.

## Does

- Run FFmpeg to inspect a video and transcode it.
- Own the transcode document (`VideoTranscodeEntity`) and its store interface. Statuses are
  Queued, In progress, Done, and Failed.
- Own the bundled FFmpeg binaries and the process runner. `cse-codec-image` uses them to
  decode HEVC stills out of HEIC.

## Does not

- Open Mongo or serve HTTP. `cse-db-mongo` implements the store. The WAR pages it.
- Queue uploads or build JPEG posters. Those stay in the WAR.
- Store the source file. That is Mongo plus `cse-media`.
