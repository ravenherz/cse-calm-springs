package com.ravenherz.cse.dal;

import java.io.File;

/**
 * Disk cache for protected media. Implemented by the WAR ({@code CseDisk}).
 */
public interface MediaCache {

    File cachedMedia(String protectedPath);
}
