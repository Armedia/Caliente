/**
 *
 */

package com.armedia.cmf.storage.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.URIStrategy;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class LocalContentStore extends ContentStore {

	private final File baseDir;
	private final URIStrategy strategy;

	public LocalContentStore(File baseDir, URIStrategy strategy) throws StorageException {
		if (baseDir == null) { throw new IllegalArgumentException("Must provide a base directory"); }
		if (strategy == null) { throw new IllegalArgumentException("Must provide a path strategy"); }
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
		this.strategy = strategy;
	}

	@Override
	protected URI doAllocateHandleId(StoredObjectType objectType, String objectId) {
		try {
			return new URI("local", this.strategy.calculateSSP(objectType, objectId), this.strategy.calculateFragment(
				objectType, objectId));
		} catch (URISyntaxException e) {
			throw new RuntimeException(
				String.format("Failed to allocate a handle ID for %s[%s]", objectType, objectId), e);
		}
	}

	@Override
	protected final File doGetFile(URI handleId) {
		return new File(this.baseDir, handleId.getSchemeSpecificPart());
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