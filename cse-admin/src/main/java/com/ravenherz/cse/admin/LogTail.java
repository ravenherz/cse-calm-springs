package com.ravenherz.cse.admin;

import java.util.List;

/**
 * Last window of the instance log. The WAR reads the file.
 */
public interface LogTail {

    String fileName();

    String windowLabel();

    Snapshot read();

    final class Snapshot {
        private final String path;
        private final String text;
        private final long fileBytes;
        private final boolean truncated;
        private final List<String> tried;
        private final String error;

        public Snapshot(String path, String text, long fileBytes, boolean truncated,
                List<String> tried, String error) {
            this.path = path;
            this.text = text;
            this.fileBytes = fileBytes;
            this.truncated = truncated;
            this.tried = tried;
            this.error = error;
        }

        public String getPath() {
            return path;
        }

        public String getText() {
            return text;
        }

        public long getFileBytes() {
            return fileBytes;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public boolean isFound() {
            return path != null && error == null;
        }

        public List<String> getTried() {
            return tried;
        }

        public String getError() {
            return error;
        }
    }
}
