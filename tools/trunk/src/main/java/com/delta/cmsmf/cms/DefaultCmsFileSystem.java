/**
 *
 */

package com.delta.cmsmf.cms;

import java.io.File;
import java.io.IOException;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DefaultCmsFileSystem implements CmsFileSystem {

	private final File baseDir;

	/**
	 *
	 */
	public DefaultCmsFileSystem(File baseDir) {
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