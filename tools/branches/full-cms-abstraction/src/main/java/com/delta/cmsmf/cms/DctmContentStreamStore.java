/**
 *
 */

package com.delta.cmsmf.cms;

import java.io.File;
import java.io.IOException;

import com.armedia.cmf.storage.ContentStreamStore;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmContentStreamStore implements ContentStreamStore {

	private final File baseDir;

	/**
	 *
	 */
	public DctmContentStreamStore(File baseDir) {
		if (baseDir == null) { throw new IllegalArgumentException("Must provide a base directory"); }
		this.baseDir = baseDir;
	}

	@Override
	public File getContentFile(File relativeFile) throws IOException {
		if (relativeFile == null) { throw new IllegalArgumentException("Must provide a file to locate"); }
		if (relativeFile.isAbsolute()) { return relativeFile.getCanonicalFile(); }
		return new File(this.baseDir, relativeFile.getPath()).getCanonicalFile();
	}

}