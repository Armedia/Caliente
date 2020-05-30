package com.armedia.caliente.engine.exporter.content;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.tools.ContentTools;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.commons.utilities.function.CheckedBiFunction;
import com.armedia.commons.utilities.function.CheckedSupplier;

public abstract class ContentExtractor implements AutoCloseable {

	protected static interface FSWriter extends CheckedBiFunction<Path, Integer, Long, Exception> {
	}

	public abstract class ContentData {

		public static final int MIN_BUFFER_SIZE = 4 * (int) FileUtils.ONE_KB;
		public static final int DEF_BUFFER_SIZE = 8 * ContentData.MIN_BUFFER_SIZE;

		private final CmfContentStream info;
		private final FSWriter fsWriter;

		protected ContentData(CmfContentStream info) {
			this(info, null);
		}

		protected ContentData(CmfContentStream info, FSWriter fileWriter) {
			this.info = info;
			this.fsWriter = fileWriter;
		}

		private int sanitizeBufferSize(int bufferSize) {
			return (bufferSize <= 0) ? ContentData.DEF_BUFFER_SIZE : Math.max(ContentData.MIN_BUFFER_SIZE, bufferSize);
		}

		public final CmfContentStream getInfo() {
			return this.info;
		}

		private long writeToChannel(CheckedSupplier<WritableByteChannel, IOException> supplier, int bufferSize)
			throws IOException, ExportException {
			bufferSize = sanitizeBufferSize(bufferSize);
			try (ReadableByteChannel in = openContentChannel()) {
				try (WritableByteChannel out = supplier.getChecked()) {
					return ContentTools.copy(in, out, bufferSize);
				}
			}
		}

		protected abstract ReadableByteChannel openContentChannel() throws IOException, ExportException;

		private CheckedSupplier<WritableByteChannel, IOException> openTargetChannel(Path target) {
			return () -> Files.newByteChannel(target, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.APPEND);
		}

		public final long saveContentTo(OutputStream out) throws IOException, ExportException {
			return saveContentTo(out, 0);
		}

		public final long saveContentTo(OutputStream out, int bufferSize) throws IOException, ExportException {
			Objects.requireNonNull(out, "Must provide an OutputStream to write to");
			return saveContentTo(Channels.newChannel(out), bufferSize);
		}

		public final long saveContentTo(File target) throws IOException, ExportException {
			return saveContentTo(target, 0);
		}

		public final long saveContentTo(File target, int bufferSize) throws IOException, ExportException {
			Objects.requireNonNull(target, "Must provide a target File");
			return saveContentTo(target.toPath(), bufferSize);
		}

		public final long saveContentTo(WritableByteChannel out) throws IOException, ExportException {
			return saveContentTo(out, 0);
		}

		public final long saveContentTo(WritableByteChannel out, int bufferSize) throws IOException, ExportException {
			Objects.requireNonNull(out, "Must provide a WritableByteChannel to write to");
			return writeToChannel(() -> out, bufferSize);
		}

		public final long saveContentTo(Path target) throws IOException, ExportException {
			return saveContentTo(target, 0);
		}

		public final long saveContentTo(Path target, int bufferSize) throws IOException, ExportException {
			Objects.requireNonNull(target, "Must provide a target Path");
			if (this.fsWriter != null) { return this.fsWriter.apply(target, sanitizeBufferSize(bufferSize)); }
			return writeToChannel(openTargetChannel(target), bufferSize);
		}
	}

	public abstract <VALUE, CONTEXT extends ExportContext<?, VALUE, ?>> Stream<? extends ContentData> getContentData(
		CONTEXT ctx, CmfObjectRef object, boolean includeRenditions) throws ExportException;
}