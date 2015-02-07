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

import org.apache.commons.io.FileUtils;

import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.URIStrategy;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class LocalContentStore extends ContentStore {

	private static final String SCHEME = "local";

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
	protected boolean isSupportedURI(URI uri) {
		return LocalContentStore.SCHEME.equals(uri.getScheme());
	}

	@Override
	protected URI doAllocateHandleId(StoredObject<?> object, String qualifier) {
		try {
			return new URI(LocalContentStore.SCHEME, this.strategy.getSSP(object), this.strategy.calculateFragment(
				object, qualifier));
		} catch (URISyntaxException e) {
			throw new RuntimeException(String.format("Failed to allocate a handle ID for %s[%s]", object.getType(),
				object.getId()), e);
		}
	}

	@Override
	protected final File doGetFile(URI handleId) {
		String ssp = handleId.getSchemeSpecificPart();
		String frag = handleId.getFragment();
		String path = (frag != null ? String.format("%s%s", ssp, frag) : ssp);
		return new File(this.baseDir, path);
	}

	@Override
	protected InputStream doOpenInput(URI handleId) throws IOException {
		return new FileInputStream(getFile(handleId));
	}

	@Override
	protected OutputStream doOpenOutput(URI handleId) throws IOException {
		File f = getFile(handleId);
		f.getParentFile().mkdirs(); // Create the parents, if needed
		if (f.createNewFile() || f.exists()) { return new FileOutputStream(f); }
		throw new IOException(String.format("Failed to create the non-existent target file [%s]", f.getAbsolutePath()));
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

	@Override
	protected boolean isSupportsFileAccess() {
		return true;
	}

	@Override
	protected void doClearAllStreams() {
		for (File f : this.baseDir.listFiles()) {
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				// Ignore it, keep going
			}
		}
	}
}