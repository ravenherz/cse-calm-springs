package com.ravenherz.cse.controller;

import com.ravenherz.cse.admin.LogTail;
import com.ravenherz.cse.engine.io.LogFileTail;
import org.springframework.stereotype.Component;

@Component
public class LogTailAdapter implements LogTail {

    private final LogFileTail logFileTail;

    public LogTailAdapter(LogFileTail logFileTail) {
        this.logFileTail = logFileTail;
    }

    @Override
    public String fileName() {
        return LogFileTail.LOG_FILE_NAME;
    }

    @Override
    public String windowLabel() {
        return LogFileTail.WINDOW_LABEL;
    }

    @Override
    public Snapshot read() {
        LogFileTail.Snapshot source = logFileTail.read();
        return new Snapshot(source.getPath(), source.getText(), source.getFileBytes(),
                source.isTruncated(), source.getTried(), source.getError());
    }
}
