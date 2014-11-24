package com.armedia.cmf.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;

import com.armedia.commons.utilities.Tools;

public abstract class ContentStore extends Store {

	protected static final int MIN_BUFFER_SIZE = 4096;
	public static final int DEFAULT_BUFFER_SIZE = 16384;
	protected static final int MAX_BUFFER_SIZE = (Integer.MAX_VALUE & 0xFFFFF000);

	public static final String DEFAULT_QUALIFIER = "content";

	public static final class Handle {
		private final ContentStore sourceStore;
		private final StoredObjectType objectType;
		private final String objectId;
		private final String qualifier;
		private final URI uri;

		private Handle(ContentStore sourceStore, StoredObjectType objectType, String objectId, String qualifier, URI uri) {
			if (sourceStore == null) { throw new IllegalArgumentException("Must provide a source ContentStore"); }
			if (objectType == null) { throw new IllegalArgumentException("Must provide an object type"); }
			if (objectId == null) { throw new IllegalArgumentException("Must provide an object id"); }
			if (qualifier == null) { throw new IllegalArgumentException("Must provide a content qualifier"); }
			if (uri == null) { throw new IllegalArgumentException(
				"Must provide a URI to identify the content within the store"); }
			this.sourceStore = sourceStore;
			this.objectType = objectType;
			this.objectId = objectId;
			this.qualifier = qualifier;
			this.uri = uri;
		}

		/**
		 * <p>
		 * Returns the type of object whose content this handle points to.
		 * </p>
		 *
		 * @return the type of object whose content this handle points to.
		 */
		public StoredObjectType getObjectType() {
			return this.objectType;
		}

		/**
		 * <p>
		 * Returns the ID for the object whose content this handle points to.
		 * </p>
		 *
		 * @return the ID for the object whose content this handle points to.
		 */
		public String getObjectId() {
			return this.objectId;
		}

		/**
		 * <p>
		 * Returns the qualifier for this content stream. Can be {@code null}.
		 * </p>
		 *
		 * @return the qualifier for this content stream
		 */
		public String getQualifier() {
			return this.qualifier;
		}

		/**
		 * <p>
		 * Returns the {@link ContentStore} from which this handle was obtained.
		 * </p>
		 *
		 * @return the {@link ContentStore} from which this handle was obtained.
		 */
		public ContentStore getSourceStore() {
			return this.sourceStore;
		}

		/**
		 * <p>
		 * Returns a {@link File} object that leads to the actual content file (existent or not),
		 * and can be used to read from and write to it. If the underlying {@link ContentStore}
		 * doesn't support this functionality, {@code null} is returned.
		 * </p>
		 * <p>
		 * Whatever file is returned will already be canonical (via {@link File#getCanonicalFile()}
		 * ), except if the invocation raises an exception. In that event, the non-canonical
		 * {@link File} will be returned.
		 * </p>
		 *
		 * @return a {@link File} object that leads to the actual content file (existent or not), or
		 *         {@code null} if this functionality is not supported.
		 */
		public File getFile() {
			return this.sourceStore.getFile(this.uri);
		}

		/**
		 * <p>
		 * Returns an {@link InputStream} that can be used to read from the actual content file, or
		 * {@code null} if this handle refers to an as-yet non-existent content stream.
		 * </p>
		 * <p>
		 * All {@link ContentStore} implementations <b>must</b> support this functionality.
		 * </p>
		 *
		 * @return an {@link InputStream} that can be used to read from the actual content file, or
		 *         {@code null} if this handle refers to an as-yet non-existent content stream
		 * @throws IOException
		 */
		public InputStream openInput() throws IOException {
			return this.sourceStore.openInput(this.uri);
		}

		/**
		 * <p>
		 * Read the contents of the given file into the underlying {@link ContentStore}, to be
		 * stored as the content for this handle, using a default read buffer size (from
		 * {@link ContentStore#DEFAULT_BUFFER_SIZE}). Supports files larger than 2GB.
		 * </p>
		 *
		 * @param source
		 *            the file to read from
		 * @return the number of bytes copied
		 * @throws IOException
		 */
		public final long readFile(File source) throws IOException {
			return readFile(source, 0);
		}

		/**
		 * <p>
		 * Read the contents of the given file into the underlying {@link ContentStore}, to be
		 * stored as the content for this handle, using a read buffer of {@code bufferSize} bytes.
		 * Supports files larger than 2GB. If {@code bufferSize} is less than or equal to 0, a
		 * default buffer of DEFAULT_BUFFER_SIZE is used.
		 * </p>
		 *
		 * @param source
		 *            the file to read from
		 * @return the number of bytes copied
		 * @throws IOException
		 */
		public final long readFile(File source, int bufferSize) throws IOException {
			if (source == null) { throw new IllegalArgumentException("Must provide a file to read from"); }
			return runTransfer(new FileInputStream(source), openOutput(), bufferSize);
		}

		/**
		 * <p>
		 * Write the content from the underlying {@link ContentStore} into the given file, using a
		 * default read buffer size (from {@link ContentStore#DEFAULT_BUFFER_SIZE}). Supports files
		 * larger than 2GB. used.
		 * </p>
		 *
		 * @param target
		 *            the file to write to
		 * @return the number of bytes copied
		 * @throws IOException
		 */
		public final long writeFile(File target) throws IOException {
			return writeFile(target, 0);
		}

		/**
		 * <p>
		 * Write the content from the underlying {@link ContentStore} into the given file, using a
		 * read buffer of {@code bufferSize} bytes. Supports files larger than 2GB. If
		 * {@code bufferSize} is less than or equal to 0, a default buffer of DEFAULT_BUFFER_SIZE is
		 * used.
		 * </p>
		 *
		 * @param target
		 *            the file to write to
		 * @return the number of bytes copied
		 * @throws IOException
		 */
		public final long writeFile(File target, int bufferSize) throws IOException {
			if (target == null) { throw new IllegalArgumentException("Must provide a file to write to"); }
			return runTransfer(openInput(), new FileOutputStream(target), bufferSize);
		}

		private long runTransfer(InputStream in, OutputStream out, int bufferSize) throws IOException {
			if (bufferSize <= 0) {
				bufferSize = ContentStore.DEFAULT_BUFFER_SIZE;
			} else {
				bufferSize = Tools
					.ensureBetween(ContentStore.MIN_BUFFER_SIZE, bufferSize, ContentStore.MAX_BUFFER_SIZE);
			}
			try {
				// We use copyLarge() to support files greater than 2GB
				return IOUtils.copyLarge(in, out, new byte[bufferSize]);
			} finally {
				IOUtils.closeQuietly(in);
				IOUtils.closeQuietly(out);
			}
		}

		/**
		 * <p>
		 * Returns an {@link OutputStream} that can be used to write to the actual content file,
		 * creating a new stream if it doesn't exist. If the underlying content store doesn't
		 * support the modification or creation of new content streams, {@code null} will be
		 * returned.
		 * </p>
		 * <p>
		 * All {@link ContentStore} implementations <b>must</b> support this functionality.
		 * </p>
		 *
		 * @return an {@link OutputStream} that can be used to write to the actual content file
		 * @throws IOException
		 */
		public OutputStream openOutput() throws IOException {
			return this.sourceStore.openOutput(this.uri);
		}

		/**
		 * <p>
		 * Returns {@code true} if this content stream already exists, or {@code false} if this
		 * handle is simply a placeholder.
		 * </p>
		 *
		 * @return {@code true} if this content stream already exists, or {@code false} if this
		 *         handle is simply a placeholder
		 */
		public boolean isExists() {
			return this.sourceStore.isExists(this.uri);
		}

		/**
		 * <p>
		 * Returns the length in bytes for the underlying content stream, or -1 if it doesn't exist
		 * (i.e. if invoking {@link #isExists()} would return {@code false}).
		 * </p>
		 *
		 * @return the length in bytes for the underlying content stream, or -1 if it doesn't exist
		 */
		public long getStreamSize() {
			return this.sourceStore.getStreamSize(this.uri);
		}
	}

	protected abstract boolean isSupportedURI(URI uri);

	protected abstract boolean isSupportsFileAccess();

	protected final void validateURI(URI uri) {
		if (uri == null) { throw new IllegalArgumentException("Must provide a URI to validate"); }
		if (!isSupportedURI(uri)) { throw new IllegalArgumentException(String.format(
			"The URI [%s] is not supported by ContentStore class [%s]", uri, getClass().getCanonicalName())); }
	}

	protected final Handle constructHandle(StoredObjectType objectType, String objectId, String qualifier, URI uri) {
		return new Handle(this, objectType, objectId, qualifier, uri);
	}

	protected final URI extractURI(Handle handle) {
		if (handle == null) { throw new IllegalArgumentException("Must provide a handle whose URI to extract"); }
		return handle.uri;
	}

	public final Handle getHandle(StoredObject<?> object, String qualifier) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to examine"); }
		return getHandle(object.getType(), object.getId(), ContentStore.DEFAULT_QUALIFIER);
	}

	public final Handle getHandle(StoredObjectType objectType, String objectId, String qualifier) {
		if (objectType == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object ID"); }
		if (qualifier == null) { throw new IllegalArgumentException("Must provide content qualifier"); }
		return constructHandle(objectType, objectId, qualifier, allocateHandleId(objectType, objectId, qualifier));
	}

	protected final URI allocateHandleId(StoredObjectType objectType, String objectId, String qualifier) {
		if (objectType == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object ID"); }
		if (qualifier == null) { throw new IllegalArgumentException("Must provide content qualifier"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doAllocateHandleId(objectType, objectId, qualifier);
		} finally {
			getReadLock().unlock();
		}
	}

	protected final File getFile(URI uri) {
		if (uri == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		// Short-cut, no need to luck if we won't do anything
		if (!isSupportsFileAccess()) { return null; }
		validateURI(uri);
		getReadLock().lock();
		try {
			assertOpen();
			File ret = doGetFile(uri);
			try {
				return ret.getCanonicalFile();
			} catch (IOException e) {
				// Can't canonicalize
				if (this.log.isTraceEnabled()) {
					this.log.error(String.format("Error canonicalizing file [%s] obtained from URI [%s]",
						ret.getAbsolutePath(), uri.toString()), e);
				}
				return ret;
			}
		} finally {
			getReadLock().unlock();
		}
	}

	protected final InputStream openInput(URI uri) throws IOException {
		if (uri == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateURI(uri);
		getReadLock().lock();
		try {
			assertOpen();
			return doOpenInput(uri);
		} finally {
			getReadLock().unlock();
		}
	}

	protected final OutputStream openOutput(URI uri) throws IOException {
		if (uri == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateURI(uri);
		getReadLock().lock();
		try {
			assertOpen();
			return doOpenOutput(uri);
		} finally {
			getReadLock().unlock();
		}
	}

	protected final boolean isExists(URI uri) {
		if (uri == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateURI(uri);
		getReadLock().lock();
		try {
			assertOpen();
			return doIsExists(uri);
		} finally {
			getReadLock().unlock();
		}
	}

	protected final long getStreamSize(URI uri) {
		if (uri == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateURI(uri);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetStreamSize(uri);
		} finally {
			getReadLock().unlock();
		}
	}

	public final void clearAllStreams() {
		getWriteLock().lock();
		try {
			assertOpen();
			doClearAllStreams();
		} finally {
			getWriteLock().unlock();
		}
	}

	protected abstract URI doAllocateHandleId(StoredObjectType objectType, String objectId, String qualifier);

	protected abstract File doGetFile(URI uri);

	protected abstract InputStream doOpenInput(URI uri) throws IOException;

	protected abstract OutputStream doOpenOutput(URI uri) throws IOException;

	protected abstract boolean doIsExists(URI uri);

	protected abstract long doGetStreamSize(URI uri);

	protected abstract void doClearAllStreams();
}