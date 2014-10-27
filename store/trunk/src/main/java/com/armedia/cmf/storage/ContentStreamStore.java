package com.armedia.cmf.storage;

import java.io.File;
import java.io.IOException;

public interface ContentStreamStore {

	/**
	 * <p>
	 * Returns the actual, canonicalized path for a content file given a relative path for the file.
	 * </p>
	 *
	 * @param relativeFile
	 * @return the actual, canonicalized path for a content file given a relative path for the file.
	 * @throws IOException
	 */
	public File getContentFile(File relativeFile) throws IOException;

}