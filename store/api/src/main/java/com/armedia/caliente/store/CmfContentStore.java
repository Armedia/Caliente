/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.ReadableByteChannelWrapper;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.WritableByteChannelWrapper;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.function.CheckedRunnable;
import com.armedia.commons.utilities.function.CheckedSupplier;

public abstract class CmfContentStore<LOCATOR, OPERATION extends CmfStoreOperation<?>> extends CmfStore<OPERATION> {

	protected static final int MIN_BUFFER_SIZE = 4096;
	public static final int DEFAULT_BUFFER_SIZE = 16384;
	protected static final int MAX_BUFFER_SIZE = (Integer.MAX_VALUE & 0x7FFFF000);

	public static final String DEFAULT_QUALIFIER = "content";

	public final class Handle {
		private final String locator;
		private final CmfContentStream info;

		protected Handle(CmfContentStream info, String locator) {
			if (info == null) { throw new IllegalArgumentException("Must provide a content info"); }
			if (StringUtils.isBlank(locator)) {
				throw new IllegalArgumentException(
					"Must provide a valid, non-blank locator string to identify the content within the store");
			}
			this.info = info;
			this.locator = locator;
			info.setLocator(this.locator);
		}

		public String getLocator() {
			return this.locator;
		}

		public CmfContentStream getInfo() {
			return this.info;
		}

		public WritableByteChannel createChannel() throws CmfStorageException {
			return CmfContentStore.this.createChannel(this);
		}

		public OutputStream createStream() throws CmfStorageException {
			return CmfContentStore.this.createStream(this);
		}

		public ReadableByteChannel openChannel() throws CmfStorageException {
			return CmfContentStore.this.openChannel(this);
		}

		public InputStream openStream() throws CmfStorageException {
			return CmfContentStore.this.openStream(this);
		}

		public long store(ReadableByteChannel in) throws CmfStorageException {
			return CmfContentStore.this.store(this, in);
		}

		public long store(InputStream in) throws CmfStorageException {
			return CmfContentStore.this.store(this, in);
		}

		public long store(File f) throws CmfStorageException {
			return CmfContentStore.this.store(this, f);
		}

		public long store(Path p) throws CmfStorageException {
			return CmfContentStore.this.store(this, p);
		}

		public long read(WritableByteChannel out) throws CmfStorageException {
			return CmfContentStore.this.read(this, out);
		}

		public long read(OutputStream out) throws CmfStorageException {
			return CmfContentStore.this.read(this, out);
		}

		public long read(File f) throws CmfStorageException {
			return CmfContentStore.this.read(this, f);
		}

		public long read(Path p) throws CmfStorageException {
			return CmfContentStore.this.read(this, p);
		}

		public File getFile() throws CmfStorageException {
			return getFile(false);
		}

		public final File getFile(boolean ensureParents) throws CmfStorageException {
			Path p = getPath(ensureParents);
			if (p == null) { return null; }
			return p.toFile();
		}

		public Path getPath() throws CmfStorageException {
			return getPath(false);
		}

		public final Path getPath(boolean ensureParents) throws CmfStorageException {
			Path p = CmfContentStore.this.getPath(this);
			if (ensureParents) {
				CmfContentStore.ensureParentExists(p);
			}
			return p;
		}

		public long getSize() throws CmfStorageException {
			return CmfContentStore.this.getSize(this);
		}

		public CmfContentStore<LOCATOR, OPERATION> getSourceStore() {
			return CmfContentStore.this;
		}

		@Override
		public String toString() {
			return String.format("Handle [id=%s, info=%s]", this.locator, this.info.getObject());
		}
	}

	public CmfContentStore(CmfStore<?> parent) {
		super(parent, "content");
	}

	protected abstract boolean isSupported(LOCATOR locator);

	public abstract boolean isSupportsFileAccess();

	// Return null if this doesn't support file access
	public final File getRootLocation() {
		if (!isSupportsFileAccess()) { return null; }
		return doGetRootLocation();
	}

	protected abstract File doGetRootLocation();

	private static void ensureParentExists(Path p) throws CmfStorageException {
		if (p == null) { throw new IllegalArgumentException("Must provide a valid path to check against"); }
		Path parent = p.getParent();
		if (parent == null) { return; }

		if (!Files.exists(parent)) {
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
					FileUtils.forceMkdir(parent.toFile());
					break;
				} catch (IOException e) {
					// Something went wrong...
					caught = e;
				}
			}
			if (caught != null) {
				throw new CmfStorageException(
					String.format("Failed to create the parent content directory [%s]", parent), caught);
			}
		}

		if (!Files.isDirectory(parent)) {
			throw new CmfStorageException(String.format("The parent location [%s] is not a directory", parent));
		}
	}

	protected final LOCATOR getLocator(Handle handle) {
		Objects.requireNonNull(handle, "Must provide a non-null Handle instance");
		try (SharedAutoLock lock = autoSharedLock()) {
			assertOpen();
			if (this != handle.getSourceStore()) {
				throw new IllegalArgumentException("The given Handle instance does not refer to content in this store");
			}
			LOCATOR locator = decodeLocator(handle.locator);
			if (locator == null) {
				throw new IllegalArgumentException("The given handle did not match an existing locator");
			}
			if (!isSupported(locator)) {
				throw new IllegalArgumentException(
					String.format("The locator [%s] (from %s)is not supported by CmfContentStore class [%s]", locator,
						handle, getClass().getCanonicalName()));
			}
			return locator;
		}
	}

	public final <VALUE> Handle addContentStream(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to examine"); }
		if (info == null) { throw new IllegalArgumentException("Must provide content info object"); }
		try (SharedAutoLock lock = autoSharedLock()) {
			assertOpen();
			LOCATOR locator = calculateLocator(translator, object, info);
			return new Handle(info, encodeLocator(locator));
		}
	}

	protected abstract String encodeLocator(LOCATOR locator);

	protected abstract LOCATOR decodeLocator(String handleId);

	/**
	 * <p>
	 * Returns the {@link Handle} for the given {@link CmfContentStream} instance if one exists in
	 * this content store, otherwise returns {@code null}.
	 * </p>
	 *
	 * @param info
	 *            the stream to retrieve the content for
	 * @return the {@link Handle} for the given {@link CmfContentStream} instance, or {@code null}
	 *         if none exists in this content store
	 */
	public final Handle findHandle(CmfContentStream info) {
		try (SharedAutoLock lock = autoSharedLock()) {
			assertOpen();
			String encodedLocator = info.getLocator();
			if (StringUtils.isBlank(encodedLocator)) {
				throw new IllegalArgumentException("The given content stream instance lacks valid locator information");
			}
			LOCATOR locator = decodeLocator(encodedLocator);
			if (locator == null) {
				throw new IllegalArgumentException(
					String.format("Failed to decode the given locator string [%s]", encodedLocator));
			}
			return new Handle(info, encodeLocator(locator));
		}
	}

	protected final Path getPath(Handle handle) throws CmfStorageException {
		try (SharedAutoLock lock = autoSharedLock()) {
			assertOpen();

			// Short-cut, no need to look if we won't do anything
			if (!isSupportsFileAccess()) { return null; }

			LOCATOR locator = getLocator(handle);
			Path p = getPath(locator);
			if (p == null) {
				throw new IllegalStateException("getPath() returned null - did you forget to override the method?");
			}
			return Tools.canonicalize(p);
		} catch (IOException e) {
			throw new CmfStorageException(String.format("Failed to locate the file for %s", handle), e);
		}
	}

	protected Path getPath(LOCATOR locator) throws IOException {
		return null;
	}

	protected final <VALUE> LOCATOR calculateLocator(CmfAttributeTranslator<VALUE> translator, CmfObject<VALUE> object,
		CmfContentStream info) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object"); }
		if (info == null) { throw new IllegalArgumentException("Must provide content info object"); }
		return doCalculateLocator(translator, object, info);
	}

	protected abstract <VALUE> LOCATOR doCalculateLocator(CmfAttributeTranslator<VALUE> translator,
		CmfObject<VALUE> object, CmfContentStream info);

	protected final InputStream openStream(Handle handle) throws CmfStorageException {
		return Channels.newInputStream(openChannel(handle));
	}

	protected final ReadableByteChannel openChannel(Handle handle) throws CmfStorageException {
		final Lock lock = acquireSharedLock();

		assertOpen();
		final LOCATOR locator = getLocator(handle);

		final OPERATION op;

		ReadableByteChannel in = null;
		boolean ok = false;
		try {
			// First, let's try a shortcut
			if (isSupportsFileAccess()) {
				op = null;
				final Path p;
				try {
					p = getPath(locator);
				} catch (IOException e) {
					throw new CmfStorageException(String.format("Failed to locate the file for locator [%s]", locator),
						e);
				}
				CmfContentStore.ensureParentExists(p);
				try {
					in = FileChannel.open(p, StandardOpenOption.READ);
				} catch (IOException e) {
					throw new CmfStorageException(
						String.format("Failed to open a readable byte channel to the file [%s]", p), e);
				}
			} else {
				op = newOperation(false);
				in = openChannel(op, locator);
			}
			ok = true;
		} finally {
			if (!ok) {
				lock.unlock();
			}
		}

		return new ReadableByteChannelWrapper(in) {
			final boolean tx = ((op != null) && op.begin());
			boolean ok = true;

			private <V, T extends Throwable> V doWork(CheckedSupplier<V, T> r) throws T {
				boolean o = false;
				try {
					V v = r.getChecked();
					o = true;
					return v;
				} finally {
					this.ok &= o;
				}
			}

			private <T extends Throwable> void doWork(CheckedRunnable<T> r) throws T {
				boolean o = false;
				try {
					r.runChecked();
					o = true;
				} finally {
					this.ok &= o;
				}
			}

			@Override
			public int read(ByteBuffer dst) throws IOException {
				return doWork(() -> super.read(dst));
			}

			@Override
			public boolean isOpen() {
				return doWork(super::isOpen);
			}

			@Override
			public void close() throws IOException {
				try {
					doWork(super::close);
					if (this.tx) {
						doWork(op::rollback);
					}
				} catch (CmfOperationException e) {
					CmfContentStore.this.log.warn(
						"Failed to roll back the transaction for setting the reading the content from locator [{}]",
						locator, e);
				} finally {
					lock.unlock();
				}
			}
		};

	}

	protected abstract ReadableByteChannel openChannel(OPERATION operation, LOCATOR locator) throws CmfStorageException;

	protected final long store(Handle handle, InputStream in) throws CmfStorageException {
		return store(handle, Channels.newChannel(in));
	}

	protected final long store(Handle handle, ReadableByteChannel in) throws CmfStorageException {
		return runConcurrently((operation) -> {
			assertOpen();
			LOCATOR locator = getLocator(handle);

			// First, let's try a shortcut
			if (isSupportsFileAccess()) {
				final Path p;
				try {
					p = getPath(locator);
				} catch (IOException e) {
					throw new CmfStorageException(String.format("Failed to locate the file for locator [%s]", locator),
						e);
				}
				CmfContentStore.ensureParentExists(p);

				try (FileChannel out = FileChannel.open(p, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING)) {
					return out.transferFrom(in, 0, Long.MAX_VALUE);
				} catch (FileNotFoundException e) {
					throw new CmfStorageException(String.format("Failed to open an output stream to the file [%s]", p),
						e);
				} catch (IOException e) {
					throw new CmfStorageException(String.format("Failed to copy the content from the file [%s]", p), e);
				}
			}

			// If it doesn't support file access...
			final boolean tx = operation.begin();
			boolean ok = false;
			try {
				long ret = store(operation, locator, in);
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
						this.log.warn("Failed to rollback the transaction for setting the content for locator [{}]",
							locator, e);
					}
				}
			}
		});
	}

	protected abstract long store(OPERATION operation, LOCATOR locator, ReadableByteChannel in)
		throws CmfStorageException;

	protected final long store(Handle handle, File source) throws CmfStorageException {
		return store(handle, Objects.requireNonNull(source, "Must provide a File instance").toPath());
	}

	protected final long store(Handle handle, Path source) throws CmfStorageException {
		return runConcurrently((operation) -> {
			// Shortcut!
			if (isSupportsFileAccess()) {
				Path target = getPath(handle);
				try {
					CmfContentStore.ensureParentExists(target);
					Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
					return Files.size(target);
				} catch (IOException e) {
					throw new CmfStorageException(String.format("Failed to copy the file [%s] as [%s]", source, target),
						e);
				}
			}

			try (FileChannel in = FileChannel.open(source, StandardOpenOption.READ)) {
				return store(handle, in);
			} catch (IOException e) {
				throw new CmfStorageException(String.format("Failed to store the content into the Path [%s]", source),
					e);
			}
		});
	}

	protected final long read(Handle handle, OutputStream out) throws CmfStorageException {
		return read(handle, Channels.newChannel(out));
	}

	protected final long read(Handle handle, WritableByteChannel out) throws CmfStorageException {
		return runConcurrently((operation) -> {
			final boolean tx = operation.begin();
			boolean ok = false;
			// First things first: get the readable channel
			try (ReadableByteChannel in = openChannel(handle)) {

				// Can we optimize this through file I/O?
				long total = 0;
				if (FileChannel.class.isInstance(out)) {
					total = FileChannel.class.cast(out).transferFrom(in, 0, handle.getInfo().getLength());
				} else {
					total = transfer(in, out);
				}
				ok = true;
				return total;
			} catch (IOException e) {
				throw new CmfStorageException(
					String.format("Failed to perform a file-level channel transfer for %s", handle), e);
			} finally {
				if (tx && !ok) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for reading the content for handle [{}]",
							handle, e);
					}
				}
			}
		});
	}

	protected final long read(Handle handle, File target) throws CmfStorageException {
		return read(handle, Objects.requireNonNull(target, "Must provide a File instance").toPath());
	}

	protected final long read(Handle handle, Path target) throws CmfStorageException {
		return runConcurrently((operation) -> {
			// Shortcut!
			if (isSupportsFileAccess()) {
				Path source = getPath(handle);
				try {
					CmfContentStore.ensureParentExists(target);
					Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
					return Files.size(target);
				} catch (IOException e) {
					throw new CmfStorageException(String.format("Failed to copy the file [%s] as [%s]", source, target),
						e);
				}
			}

			try (FileChannel out = FileChannel.open(target, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.APPEND, StandardOpenOption.WRITE)) {
				return read(handle, out);
			} catch (IOException e) {
				throw new CmfStorageException(String.format("Failed to store the content into the Path [%s]", target),
					e);
			}
		});
	}

	protected int getBufferSize() {
		return CmfContentStore.MIN_BUFFER_SIZE;
	}

	protected final long transfer(ReadableByteChannel in, WritableByteChannel out) throws IOException {
		int bufferSize = (getBufferSize() & CmfContentStore.MAX_BUFFER_SIZE);
		// No dice... let's do the manual copy
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		long total = 0;
		while (true) {
			buffer.clear();
			int r = in.read(buffer);
			if (r < 0) {
				break;
			}
			total += r;
			if (r > 0) {
				buffer.flip();
				out.write(buffer);
				total += r;
			}
		}
		return total;
	}

	protected final OutputStream createStream(Handle handle) throws CmfStorageException {
		return Channels.newOutputStream(createChannel(handle));
	}

	protected final WritableByteChannel createChannel(Handle handle) throws CmfStorageException {
		final Lock lock = acquireSharedLock();

		assertOpen();
		final LOCATOR locator = getLocator(handle);

		final OPERATION op;

		WritableByteChannel out = null;
		boolean ok = false;
		try {
			// First, let's try a shortcut
			if (isSupportsFileAccess()) {
				op = null;
				final Path p;
				try {
					p = getPath(locator);
				} catch (IOException e) {
					throw new CmfStorageException(String.format("Failed to locate the file for locator [%s]", locator),
						e);
				}
				CmfContentStore.ensureParentExists(p);
				try {
					out = FileChannel.open(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.APPEND);
				} catch (IOException e) {
					throw new CmfStorageException(
						String.format("Failed to open a writable byte channel to the file [%s]", p), e);
				}
			} else {
				op = newOperation(false);
				out = createChannel(op, locator);
			}
			ok = true;
		} finally {
			if (!ok) {
				lock.unlock();
			}
		}

		return new WritableByteChannelWrapper(out) {
			final boolean tx = ((op != null) && op.begin());
			boolean ok = true;

			private <V, T extends Throwable> V doWork(CheckedSupplier<V, T> r) throws T {
				boolean o = false;
				try {
					V v = r.getChecked();
					o = true;
					return v;
				} finally {
					this.ok &= o;
				}
			}

			private <T extends Throwable> void doWork(CheckedRunnable<T> r) throws T {
				boolean o = false;
				try {
					r.runChecked();
					o = true;
				} finally {
					this.ok &= o;
				}
			}

			@Override
			public int write(ByteBuffer src) throws IOException {
				return doWork(() -> super.write(src));
			}

			@Override
			public boolean isOpen() {
				return doWork(super::isOpen);
			}

			@Override
			public void close() throws IOException {
				try {
					doWork(super::close);
					if (this.tx && this.ok) {
						doWork(op::commit);
					}
				} catch (CmfOperationException e) {
					this.ok = false;
					CmfContentStore.this.log
						.warn("Failed to commit the transaction for setting the content for locator [{}]", locator, e);
				} finally {
					if (this.tx && !this.ok) {
						try {
							op.rollback();
						} catch (CmfStorageException e) {
							CmfContentStore.this.log.warn(
								"Failed to rollback the transaction for setting the content for locator [{}]", locator,
								e);
						}
					}
					lock.unlock();
				}
			}
		};
	}

	protected abstract WritableByteChannel createChannel(OPERATION operation, LOCATOR locator)
		throws CmfStorageException;

	public final boolean exists(Handle handle) throws CmfStorageException {
		return runConcurrently((operation) -> {
			assertOpen();
			LOCATOR locator = getLocator(handle);

			// First, let's try a shortcut
			if (isSupportsFileAccess()) {
				try {
					return Files.exists(getPath(locator));
				} catch (IOException e) {
					throw new CmfStorageException(String.format("Failed to locate the file for locator [%s]", locator),
						e);
				}
			}

			// If it doesn't support file access...
			final boolean tx = operation.begin();
			try {
				return exists(operation, locator);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for retrieving the file locator [{}]",
							locator, e);
					}
				}
			}
		});
	}

	protected abstract boolean exists(OPERATION operation, LOCATOR locator) throws CmfStorageException;

	protected final long getSize(Handle handle) throws CmfStorageException {
		return runConcurrently((operation) -> {
			assertOpen();
			LOCATOR locator = getLocator(handle);
			// First, let's try a shortcut
			if (isSupportsFileAccess()) {
				try {
					Path p = getPath(locator);
					if (!Files.exists(p) || !Files.isRegularFile(p)) {
						throw new CmfStorageException(
							String.format("Locator [%s] doesn't refer to an existing and valid file", locator));
					}
					return Files.size(p);
				} catch (IOException e) {
					throw new CmfStorageException(String.format("Failed to locate the file for locator [%s]", locator),
						e);
				}
			}

			// If it doesn't support file access...
			final boolean tx = operation.begin();
			try {
				return getSize(operation, locator);
			} finally {
				if (tx) {
					try {
						operation.rollback();
					} catch (CmfStorageException e) {
						this.log.warn("Failed to rollback the transaction for getting the stream size for locator {}",
							locator, e);
					}
				}
			}
		});
	}

	protected abstract long getSize(OPERATION operation, LOCATOR locator) throws CmfStorageException;

	public final void clearAllStreams() throws CmfStorageException {
		runExclusively((operation) -> {
			assertOpen();
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
		});
	}

	protected abstract void clearAllStreams(OPERATION operation) throws CmfStorageException;
}