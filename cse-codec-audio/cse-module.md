# cse-codec-audio

Codec for MP3 inspection. A codec transforms bytes. It does not own entities, DAOs, or HTTP.

## Does

- Read ID3 tags and build waveform peaks.

## Does not

- Render playlist embed HTML. That stays with content embeds.
- Transcode audio or store the file bytes.
- Own a DAL. Resource documents stay with `cse-media`.
