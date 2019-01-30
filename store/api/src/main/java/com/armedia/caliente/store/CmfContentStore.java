package com.armedia.caliente.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.armedia.commons.utilities.Tools;

public abstract class CmfContentStore<LOCATOR, CONNECTION, OPERATION extends CmfStoreOperation<CONNECTION>>
	extends CmfStore<CONNECTION, OPERATION> {

	protected static final int MIN_BUFFER_SIZE = 4096;
	public static final int DEFAULT_BUFFER_SIZE = 16384;
	protected static final int MAX_BUFFER_SIZE = (Integer.MAX_VALUE & 0xFFFFF000);

	public static final String DEFAULT_QUALIFIER = "content";

	public abstract class Handle {
		private final CmfArchetype objectType;
		private final String objectId;
		private final CmfContentStream info;
		private final LOCATOR locator;

		protected Handle(CmfObject<?> object, CmfContentStream info, LOCATOR locator) {
			if (object == null) { throw new IllegalArgumentException("Must provide an object"); }
			if (info == null) { throw new IllegalArgumentException("Must provide a content info"); }
			if (locator == null) {
				throw new IllegalArgumentException(
					"Must provide a locator string to identify the content within the store");
			}
			this.objectType = object.getType();
			this.objectId = object.getId();
			this.info = info;
			this.locator = locator;
		}

		/**
		 * <p>
		 * Returns the type of object whose content this handle points to.
		 * </p>
		 *
		 * @return the type of object whose content this handle points to.
		 */
		public final CmfArchetype getObjectType() {
			return this.objectType;
		}

		/**
		 * <p>
		 * Returns the ID for the object whose content this handle points to.
		 * </p>
		 *
		 * @return the ID for the object whose content this handle points to.
		 */
		public final String getObjectId() {
			return this.objectId;
		}

		/**
		 * <p>
		 * Returns the qualifier for this content stream. Can be {@code null}.
		 * </p>
		 *
		 * @return the qualifier for this content stream
		 */
		public final CmfContentStream getInfo() {
			return this.info;
		}

		/**
		 * <p>
		 * Returns the {@link CmfContentStore} from which this handle was obtained.
		 * </p>
		 *
		 * @return the {@link CmfContentStore} from which this handle was obtained.
		 */
		public final CmfContentStore<LOCATOR, CONNECTION, OPERATION> getSourceStore() {
			return CmfContentStore.this;
		}

		/**
		 * <p>
		 * Returns a {@link File} object that leads to the actual content file (existent or not),
		 * and can be used to read from and write to it. If the underlying {@link CmfContentStore}
		 * doesn't support this functionality, {@code null} is returned.
		 * </p>
		 * <p>
		 * Whatever file is returned will already be canonical (via {@link File#getCanonicalFile()}
		 * ), except if the invocation raises an exception. In that event, the non-canonical
		 * {@link File} will be returned.
		 * </p>
		 * <p>
		 * This invocation is identical to calling {@link #getFile(boolean)} with a {@code false}
		 * parameter.
		 * </p>
		 *
		 * @return a {@link File} object that leads to the actual content file (existent or not), or
		 *         {@code null} if this functionality is not supported.
		 * @throws IOException
		 */
		public final File getFile() throws IOException {
			return getFile(false);
		}

		/**
		 * <p>
		 * Returns a {@link File} object that leads to the actual content file (existent or not),
		 * and can be used to read from and write to it. If the underlying {@link CmfContentStore}
		 * doesn't support this functionality, {@code null} is returned.
		 * </p>
		 * <p>
		 * The {@code ensureParents} parameter controls whether or not the file's parent path will
		 * be created if they don't exist (recursively). A value of {@code true} causes any missing
		 * components of the file's parent path to be created, while {@code false} does nothing.
		 * </p>
		 * <p>
		 * Whatever file is returned will already be canonical (via {@link File#getCanonicalFile()}
		 * ), except if the invocation raises an exception. In that event, the non-canonical
		 * {@link File} will be returned.
		 * </p>
		 *
		 * @return a {@link File} object that leads to the actual content file (existent or not), or
		 *         {@code null} if this functionality is not supported.
		 * @param ensureParents
		 *            whether or not to ensure the parent folders are created if they don't exist
		 * @throws IOException
		 */
		public final File getFile(boolean ensureParents) throws IOException {
			File f = CmfContentStore.this.getFile(this.locator);
			if (ensureParents) {
				ensureParentExists(f);
			}
			return f;
		}

		/**
		 * <p>
		 * Returns an {@link InputStream} that can be used to read from the actual content file, or
		 * {@code null} if this handle refers to an as-yet non-existent content stream.
		 * </p>
		 * <p>
		 * All {@link CmfContentStore} implementations <b>must</b> support this functionality.
		 * </p>
		 *
		 * @return an {@link InputStream} that can be used to read from the actual content file, or
		 *         {@code null} if this handle refers to an as-yet non-existent content stream
		 * @throws CmfStorageException
		 */
		public final InputStream openInput() throws CmfStorageException {
			return CmfContentStore.this.openInput(this.locator);
		}

		/**
		 * <p>
		 * Read the contents of the given file into the underlying {@link CmfContentStore}, to be
		 * stored as the content for this handle, using a default read buffer size (from
		 * {@link CmfContentStore#DEFAULT_BUFFER_SIZE}). Supports files larger than 2GB.
		 * </p>
		 *
		 * @param source
		 *            the file to read from
		 * @return the number of bytes copied
		 * @throws IOException
		 * @throws CmfStorageException
		 */
		public final long readFile(File source) throws IOException, CmfStorageException {
			return readFile(source, 0);
		}

		/**
		 * <p>
		 * Read the contents of the given file into the underlying {@link CmfContentStore}, to be
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
		public final long readFile(File source, int bufferSize) throws IOException, CmfStorageException {
			if (source == null) { throw new IllegalArgumentException("Must provide a file to read from"); }
			return setContents(new FileInputStream(source));
		}

		/**
		 * <p>
		 * Write the content from the underlying {@link CmfContentStore} into the given file, using
		 * a default read buffer size (from {@link CmfContentStore#DEFAULT_BUFFER_SIZE}). Supports
		 * files larger than 2GB. used.
		 * </p>
		 *
		 * @param target
		 *            the file to write to
		 * @return the number of bytes copied
		 * @throws IOException
		 */
		public final long writeFile(File target) throws IOException, CmfStorageException {
			return writeFile(target, 0);
		}

		/**
		 * <p>
		 * Write the content from the underlying {@link CmfContentStore} into the given file, using
		 * a read buffer of {@code bufferSize} bytes. Supports files larger than 2GB. If
		 * {@code bufferSize} is less than or equal to 0, a default buffer of DEFAULT_BUFFER_SIZE is
		 * used.
		 * </p>
		 *
		 * @param target
		 *            the file to write to
		 * @return the number of bytes copied
		 * @throws IOException
		 */
		public final long writeFile(File target, int bufferSize) throws IOException, CmfStorageException {
			if (target == null) { throw new IllegalArgumentException("Must provide a file to write to"); }
			try (InputStream in = openInput()) {
				try (OutputStream out = new FileOutputStream(target)) {
					if (bufferSize <= 0) {
						bufferSize = CmfContentStore.DEFAULT_BUFFER_SIZE;
					} else {
						bufferSize = Tools.ensureBetween(CmfContentStore.MIN_BUFFER_SIZE, bufferSize,
							CmfContentStore.MAX_BUFFER_SIZE);
					}
					// We use copyLarge() to support files greater than 2GB
					return IOUtils.copyLarge(in, out, new byte[bufferSize]);
				}
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
		 * All {@link CmfContentStore} implementations <b>must</b> support this functionality.
		 * </p>
		 *
		 * @return an {@link OutputStream} that can be used to write to the actual content file
		 * @throws CmfStorageException
		 */
		public final long setContents(InputStream in) throws CmfStorageException {
			return CmfContentStore.this.setContents(this.locator, in);
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
		public final boolean isExists() throws CmfStorageException {
			return CmfContentStore.this.isExists(this.locator);
		}

		/**
		 * <p>
		 * Returns the length in bytes for the underlying content stream, or -1 if it doesn't exist
		 * (i.e. if invoking {@link #isExists()} would return {@code false}).
		 * </p>
		 *
		 * @return the length in bytes for the underlying content stream, or -1 if it doesn't exist
		 * @throws CmfStorageException
		 */
		public final long getStreamSize() throws CmfStorageException {
			return CmfContentStore.this.getStreamSize(this.locator);
		}
	}

	protected abstract boolean isSupported(LOCATOR locator);

	public abstract boolean isSupportsFileAccess();

	// Return null if this doesn't support file access
	public final File getRootLocation() {
		if (!isSupportsFileAccess()) { return null; }
		return doGetRootLocation();
	}

	protected abstract File doGetRootLocation();

	protected final File getFile(LOCATOR locator) throws IOException {
		if (locator == null) { throw new IllegalArgumentException("Must provide a locator string"); }
		// Short-cut, no need to luck if we won't do anything
		if (!isSupportsFileAccess()) { return null; }
		getReadLock().lock();
		assertOpen();
		try {
			File f = doGetFile(locator);
			if (f == null) {
				throw new IllegalStateException("doGetFile() returned null - did you forget to override the method?");
			}
			return f.getCanonicalFile();
		} finally {
			getReadLock().unlock();
		}
	}

	private void ensureParentExists(File f) throws IOException {
		if (f == null) { throw new IllegalArgumentException("Must provide a valid file to check against"); }
		File parent = f.getParentFile();
		if (parent == null) { return; }

		if (!parent.exists()) {
			IOException caught = null;
			for (int i = 0; (i < 3); i++) {
				if (i > 0) {
					// Only sleep if this is a retry
					try {
						Thread.sleep(333);
					} catch (InterruptedException e2) {
						// Ignore...
					}
				}

				try {
					caught = null;
					FileUtils.forceMkdir(parent);
					break;
				} catch (IOException e) {
					// Something went wrong...
					caught = e;
				}
			}
			if (caught != null) {
				throw new IOException(
					String.format("Failed to create the parent content directory [%s]", parent.getAbsolutePath()),
					caught);
			}
		}

		if (!parent.isDirectory()) {
			throw new IOException(
				String.format("The parent location [%s] is not a directory", parent.getAbsoluteFile()));
		}
	}

	protected File doGetFile(LOCATOR locator) throws IOException {
		return null;
	}

	protected final void validateLocator(LOCATOR locator) {
		if (locator == null) { throw new IllegalArgumentException("Must provide a non-null locator"); }
		if (!isSupported(locator)) {
			throw new IllegalArgumentException(
				String.format("The locator [%s] is not supported by CmfContentStore class [%s]", locator,
					getClass().getCanonicalName()));
		}
	}

	protected abstract Handle constructHandle(CmfObject<?> object, CmfContentStream info, LOCATOR locator);

	protected final LOCATOR extractLocator(Handle handle) {
		if (handle == null) { throw new IllegalArgumentException("Must provide a handle whose locator to extract"); }
		return handle.locator;
	}

	public final <T> Handle getHandle(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to examine"); }
		if (info == null) { throw new IllegalArgumentException("Must provide content info object"); }
		return constructHandle(object, info, calculateLocator(translator, object, info));
	}

	protected final <T> LOCATOR calculateLocator(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object"); }
		if (info == null) { throw new IllegalArgumentException("Must provide content info object"); }
		return doCalculateLocator(translator, object, info);
	}

	protected abstract <T> LOCATOR doCalculateLocator(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info);

	protected final InputStream openInput(LOCATOR locator) throws CmfStorageException {
		if (locator == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateLocator(locator);

		// First, let's try a shortcut
		if (isSupportsFileAccess()) {
			final File f;
			try {
				f = getFile(locator);
			} catch (IOException e) {
				throw new CmfStorageException(String.format("Failed to locate the file for locator [%s]", locator), e);
			}
			try {
				return new FileInputStream(f);
			} catch (FileNotFoundException e) {
				throw new CmfStorageException(
					String.format("Failed to open an input stream from the file [%s]", f.getAbsolutePath()), e);
			}
		}

		// If it doesn't support file access...
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return openInput(operation, locator);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for retrieving the file locator [%s]", locator), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract InputStream openInput(OPERATION operation, LOCATOR locator) throws CmfStorageException;

	protected final long setContents(LOCATOR locator, InputStream in) throws CmfStorageException {
		if (locator == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateLocator(locator);

		// First, let's try a shortcut
		if (isSupportsFileAccess()) {
			final File f;
			try {
				f = getFile(locator);
			} catch (IOException e) {
				throw new CmfStorageException(String.format("Failed to locate the file for locator [%s]", locator), e);
			}
			try {
				ensureParentExists(f);
			} catch (IOException e) {
				throw new CmfStorageException(
					String.format("Failed to create the requisite directory structure for locator [%s]", locator), e);
			}
			try (OutputStream out = new FileOutputStream(f)) {
				return IOUtils.copyLarge(in, out);
			} catch (FileNotFoundException e) {
				throw new CmfStorageException(
					String.format("Failed to open an output stream to the file [%s]", f.getAbsolutePath()), e);
			} catch (IOException e) {
				throw new CmfStorageException(
					String.format("Failed to copy the content from the file [%s]", f.getAbsolutePath()), e);
			}
		}

		// If it doesn't support file access...
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				long ret = setContents(operation, locator, in);
				if (tx) {
					operation.commit();
				}
				ok = true;
				return ret;
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for setting the content for locator [%s]", locator), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract long setContents(OPERATION operation, LOCATOR locator, InputStream in)
		throws CmfStorageException;

	protected final boolean isExists(LOCATOR locator) throws CmfStorageException {
		if (locator == null) { throw new IllegalArgumentException("Must provide a handle ID"); }
		validateLocator(locator);
		// First, let's try a shortcut
		if (isSupportsFileAccess()) {
			try {
				getFile(locator).exists();
			} catch (IOException e) {
				throw new CmfStorageException(String.format("Failed to locate the file for locator [%s]", locator), e);
			}
		}

		// If it doesn't support file access...
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return isExists(operation, locator);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for retrieving the file locator [%s]", locator), e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract boolean isExists(OPERATION operation, LOCATOR locator) throws CmfStorageException;

	protected final long getStreamSize(LOCATOR locator) throws CmfStorageException {
		validateLocator(locator);
		// First, let's try a shortcut
		if (isSupportsFileAccess()) {
			try {
				File f = getFile(locator);
				if (!f.exists() || !f.isFile()) {
					throw new CmfStorageException(
						String.format("Locator [%s] doesn't refer to an existing and valid file", locator));
				}
				return f.length();
			} catch (IOException e) {
				throw new CmfStorageException(String.format("Failed to locate the file for locator [%s]", locator), e);
			}
		}

		// If it doesn't support file access...
		OPERATION operation = beginConcurrentInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				return getStreamSize(operation, locator);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn(String.format(
							"Failed to rollback the transaction for getting the stream size for locator %s", locator),
							e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract long getStreamSize(OPERATION operation, LOCATOR locator) throws CmfStorageException;

	public final void clearAllStreams() throws CmfStorageException {
		OPERATION operation = beginExclusiveInvocation();
		try {
			final boolean tx = operation.begin();
			try {
				clearAllStreams(operation);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for clearing all the streams", e);
					}
				}
			}
		} finally {
			endInvocation(operation);
		}
	}

	protected abstract void clearAllStreams(OPERATION operation) throws CmfStorageException;
}