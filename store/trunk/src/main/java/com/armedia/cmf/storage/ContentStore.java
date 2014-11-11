package com.armedia.cmf.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public abstract class ContentStore extends Store {

	public static final class Handle {
		private final ContentStore sourceStore;
		private final StoredObjectType objectType;
		private final String objectId;
		private final URI uri;

		private Handle(ContentStore sourceStore, StoredObjectType objectType, String objectId, URI uri) {
			this.sourceStore = sourceStore;
			this.objectType = objectType;
			this.objectId = objectId;
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
		 * Returns an implementation-specific identifier that can be used by the content store
		 * implementation to identify and retrieve the content stream for which this handle was
		 * created.
		 * </p>
		 *
		 * @return an implementation-specific identifier
		 */
		public URI getURI() {
			return this.uri;
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
		 * Returns an {@link OutputStream} that can be used to write to the actual content file,
		 * creating a new stream if it doesn't exist.
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

	protected final void validateURI(URI uri) {
		if (uri == null) { throw new IllegalArgumentException("Must provide a URI to validate"); }
		if (!isSupportedURI(uri)) { throw new IllegalArgumentException(String.format(
			"The URI [%s] is not supported by ContentStore class [%s]", uri, getClass().getCanonicalName())); }
	}

	protected final Handle constructHandle(StoredObjectType objectType, String objectId, URI handleId) {
		return new Handle(this, objectType, objectId, handleId);
	}

	public final Handle newHandle(StoredObjectType objectType, String objectId) {
		return newHandle(objectType, objectId, null);
	}

	public final Handle newHandle(StoredObjectType objectType, String objectId, String qualifier) {
		return constructHandle(objectType, objectId, allocateHandleId(objectType, objectId, qualifier));
	}

	protected final URI allocateHandleId(StoredObjectType objectType, String objectId, String qualifier) {
		if (objectType == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (objectId == null) { throw new IllegalArgumentException("Must provide an object ID"); }
		getReadLock().lock();
		try {
			assertOpen();
			return doAllocateHandleId(objectType, objectId, qualifier);
		} finally {
			getReadLock().unlock();
		}
	}

	protected final File getFile(URI handleId) {
		if (handleId == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateURI(handleId);
		getReadLock().lock();
		try {
			assertOpen();
			File ret = doGetFile(handleId);
			try {
				return ret.getCanonicalFile();
			} catch (IOException e) {
				// Can't canonicalize
			}
			return ret;
		} finally {
			getReadLock().unlock();
		}
	}

	protected final InputStream openInput(URI handleId) throws IOException {
		if (handleId == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateURI(handleId);
		getReadLock().lock();
		try {
			assertOpen();
			return doOpenInput(handleId);
		} finally {
			getReadLock().unlock();
		}
	}

	protected final OutputStream openOutput(URI handleId) throws IOException {
		if (handleId == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateURI(handleId);
		getReadLock().lock();
		try {
			assertOpen();
			return doOpenOutput(handleId);
		} finally {
			getReadLock().unlock();
		}
	}

	protected final boolean isExists(URI handleId) {
		if (handleId == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateURI(handleId);
		getReadLock().lock();
		try {
			assertOpen();
			return doIsExists(handleId);
		} finally {
			getReadLock().unlock();
		}
	}

	protected final long getStreamSize(URI handleId) {
		if (handleId == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateURI(handleId);
		getReadLock().lock();
		try {
			assertOpen();
			return doGetStreamSize(handleId);
		} finally {
			getReadLock().unlock();
		}
	}

	protected abstract URI doAllocateHandleId(StoredObjectType objectType, String objectId, String qualifier);

	protected abstract File doGetFile(URI handleId);

	protected abstract InputStream doOpenInput(URI handleId) throws IOException;

	protected abstract OutputStream doOpenOutput(URI handleId) throws IOException;

	protected abstract boolean doIsExists(URI handleId);

	protected abstract long doGetStreamSize(URI handleId);
}