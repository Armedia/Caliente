package com.armedia.caliente.tools;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Objects;

import com.armedia.commons.utilities.function.CheckedBiConsumer;

public final class FunctionalDeferredWritableChannel extends DeferredWritableChannel {

	private final CheckedBiConsumer<ReadableByteChannel, Long, IOException> processor;

	public FunctionalDeferredWritableChannel(CheckedBiConsumer<ReadableByteChannel, Long, IOException> processor)
		throws IOException {
		this(null, processor);
	}

	public FunctionalDeferredWritableChannel(Path tempFile,
		CheckedBiConsumer<ReadableByteChannel, Long, IOException> processor) throws IOException {
		super(tempFile);
		this.processor = Objects.requireNonNull(processor);
	}

	@Override
	protected void process(ReadableByteChannel in, long size) throws IOException {
		this.processor.acceptChecked(in, size);
	}
}