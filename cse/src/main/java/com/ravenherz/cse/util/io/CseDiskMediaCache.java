package com.ravenherz.cse.util.io;

import com.ravenherz.cse.dal.MediaCache;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CseDiskMediaCache implements MediaCache {

    @Override
    public File cachedMedia(String protectedPath) {
        return CseDisk.cachedMedia(protectedPath);
    }
}
