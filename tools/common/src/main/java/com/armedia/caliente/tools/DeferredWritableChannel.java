package com.armedia.caliente.tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.function.CheckedConsumer;

public class DeferredWritableChannel extends BaseShareableLockable implements WritableByteChannel {

	private final Path tempFile;
	private final CheckedConsumer<ReadableByteChannel, IOException> reader;
	private final WritableByteChannel out;

	public DeferredWritableChannel(CheckedConsumer<ReadableByteChannel, IOException> reader) throws IOException {
		this(reader, null);
	}

	public DeferredWritableChannel(CheckedConsumer<ReadableByteChannel, IOException> reader, Path tempFile)
		throws IOException {
		this.reader = Objects.requireNonNull(reader, "Must provide a consumer for the data once writing is done");
		if (tempFile != null) {
			this.tempFile = tempFile;
		} else {
			this.tempFile = Files.createTempFile(null, null);
		}
		this.out = FileChannel.open(this.tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
			StandardOpenOption.APPEND);
	}

	@Override
	public boolean isOpen() {
		return shareLocked(this.out::isOpen);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return mutexLocked(() -> this.out.write(src));
	}

	@Override
	public void close() throws IOException {
		shareLockedUpgradable(this.out::isOpen, () -> {
			try {
				// Close the output channel
				this.out.close();

				// Transfer the data over ...
				try (ReadableByteChannel in = Files.newByteChannel(this.tempFile, StandardOpenOption.READ)) {
					this.reader.acceptChecked(in);
				}
			} finally {
				// Cleanup
				FileUtils.deleteQuietly(this.tempFile.toFile());
			}
		});
	}
}