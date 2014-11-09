/**
 *
 */

package com.armedia.cmf.storage.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredObjectType;

/**
 * @author diego
 *
 */
public class SimpleFileContentStreamStore extends ContentStore {

	private final File baseDir;

	public SimpleFileContentStreamStore(File baseDir) {
		if (baseDir == null) { throw new IllegalArgumentException("Must provide a base directory"); }
		if (baseDir.exists() && !baseDir.isDirectory()) { throw new IllegalArgumentException(String.format(
			"The file at [%s] is not a directory", baseDir.getAbsolutePath())); }
		if (!baseDir.exists() && !baseDir.mkdirs()) { throw new IllegalArgumentException(String.format(
			"Failed to create the full path at [%s] ", baseDir.getAbsolutePath())); }
		File f = baseDir;
		try {
			f = baseDir.getCanonicalFile();
		} catch (IOException e) {
			f = baseDir;
		}
		this.baseDir = f;
	}

	@Override
	protected URI doAllocateHandleId(StoredObjectType objectType, String objectId) {
		return new File(String.format("%s/%s", objectType, objectId)).toURI();
	}

	@Override
	protected File doGetFile(URI handleId) {
		return new File(this.baseDir, handleId.getPath());
	}

	@Override
	protected InputStream doOpenInput(URI handleId) throws IOException {
		return new FileInputStream(getFile(handleId));
	}

	@Override
	protected OutputStream doOpenOutput(URI handleId) throws IOException {
		return new FileOutputStream(getFile(handleId));
	}

	@Override
	protected boolean doIsExists(URI handleId) {
		return getFile(handleId).exists();
	}

	@Override
	protected long doGetStreamSize(URI handleId) {
		File f = getFile(handleId);
		return (f.exists() ? f.length() : -1);
	}
}