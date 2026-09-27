package com.ravenherz.optideployer;

public record UploadResult(int status, String body) {

    public static UploadResult stored() {
        return new UploadResult(204, "");
    }

    public static UploadResult ok(String body) {
        return new UploadResult(200, body);
    }

    public static UploadResult bad(String body) {
        return new UploadResult(400, body);
    }

    public static UploadResult denied() {
        return new UploadResult(403, "forbidden");
    }

    public static UploadResult conflict(String body) {
        return new UploadResult(409, body);
    }

    public static UploadResult failed(String body) {
        return new UploadResult(502, body);
    }
}
