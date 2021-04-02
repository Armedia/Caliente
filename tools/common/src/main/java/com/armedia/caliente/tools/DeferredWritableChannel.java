package com.armedia.caliente.tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;

import com.armedia.commons.utilities.concurrent.BaseShareableLockable;

public abstract class DeferredWritableChannel extends BaseShareableLockable implements WritableByteChannel {

	private final Path tempFile;
	private final WritableByteChannel out;

	public DeferredWritableChannel() throws IOException {
		this(null);
	}

	public DeferredWritableChannel(Path tempFile) throws IOException {
		if (tempFile != null) {
			this.tempFile = tempFile;
		} else {
			this.tempFile = Files.createTempFile(null, null);
		}
		this.out = FileChannel.open(this.tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
			StandardOpenOption.APPEND);
	}

	@Override
	public final boolean isOpen() {
		return shareLocked(this.out::isOpen);
	}

	@Override
	public final int write(ByteBuffer src) throws IOException {
		return mutexLocked(() -> this.out.write(src));
	}

	protected abstract void process(ReadableByteChannel in, long length) throws IOException;

	@Override
	public final void close() throws IOException {
		shareLockedUpgradable(this.out::isOpen, () -> {
			try {
				// Close the output channel
				this.out.close();

				// Transfer the data over ...
				try (ReadableByteChannel in = Files.newByteChannel(this.tempFile, StandardOpenOption.READ)) {
					process(in, Files.size(this.tempFile));
				}
			} finally {
				// Cleanup
				FileUtils.deleteQuietly(this.tempFile.toFile());
			}
		});
	}
}