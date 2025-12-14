package com.ravenherz.rhzwe.util.io;

import com.ravenherz.rhzwe.util.Settings;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 *	ServletFile provides relative to absolute path conversion methods
 */
@Component
public class ServletFile {
	/**
	 * Convert relative path to absolute path
	 * based on web-application context directory
	 * @param relPath relative path
	 * @return absolute path
	 */
	public static File classPathFile(Settings settings, String relPath) {
		String filteredPath = relPath.startsWith("/") ? relPath.replaceFirst("/", "") : relPath;
		try {
			return ResourceUtils.getFile("classpath:" + filteredPath);
		} catch (FileNotFoundException e) {
			System.out.println(e.getMessage());
			return null;
		}
	}

	public static File classPathFile(String relPath) {
		return classPathFile(null, relPath);
	}
}
