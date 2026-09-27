package com.ravenherz.optideployer;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
public final class UploadController {

    private final UploadService uploads;

    public UploadController(UploadService uploads) {
        this.uploads = uploads;
    }

    @PostMapping(value = {"/upload-and-deploy", "/upload-and-deploy/"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(
            @RequestParam("secret-uuid") String secret,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partIndex") int partIndex,
            @RequestParam("totalSize") long totalSize,
            @RequestPart("file") MultipartFile file) throws IOException {
        UploadResult result;
        try (InputStream in = file.getInputStream()) {
            result = uploads.accept(secret, uploadId, partIndex, totalSize, in);
        }
        if (result.status() == 204) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(result.status())
                .contentType(MediaType.TEXT_PLAIN)
                .body(result.body() == null ? "" : result.body());
    }
}
