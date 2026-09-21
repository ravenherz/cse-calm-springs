# cse-video

FFmpeg probe and transcode (JavaCPP).

## Does

- Run FFmpeg to inspect a video and transcode it.

## Does not

- Queue uploads or build JPEG posters. Those stay in the WAR.
- Store the source file. That is Mongo plus `cse-media`.
