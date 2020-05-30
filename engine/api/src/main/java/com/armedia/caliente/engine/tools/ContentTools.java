package com.armedia.caliente.engine.tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;

public class ContentTools {
	public static final int MIN_BUFFER_SIZE = (4 * (int) FileUtils.ONE_KB);
	public static final int DEF_BUFFER_SIZE = (32 * (int) FileUtils.ONE_KB);

	public static long copy(ReadableByteChannel in, WritableByteChannel out) throws IOException {
		return ContentTools.copy(in, out, null);
	}

	public static long copy(ReadableByteChannel in, WritableByteChannel out, Predicate<ByteBuffer> writeFilter)
		throws IOException {
		return ContentTools.copy(in, out, ContentTools.DEF_BUFFER_SIZE, writeFilter);
	}

	public static long copy(ReadableByteChannel in, WritableByteChannel out, int bufSize) throws IOException {
		return ContentTools.copy(in, out, bufSize, null);
	}

	public static long copy(ReadableByteChannel in, WritableByteChannel out, int bufSize,
		Predicate<ByteBuffer> writeFilter) throws IOException {
		long ret = 0;
		final ByteBuffer buf = ByteBuffer.allocate(Math.max(1024, bufSize));
		while (true) {
			buf.clear();
			final int read = in.read(buf);
			if (read < 0) { return ret; }
			if (read > 0) {
				buf.flip();
				if ((writeFilter != null) && !writeFilter.test(buf.asReadOnlyBuffer())) { return ret; }
				int written = 0;
				// Is this the right way to do it? Maybe do a timeout?
				boolean yield = false;
				while (buf.hasRemaining()) {
					if (yield) {
						// Be a good citizen... but only after a "missed" write
						yield = false;
						Thread.yield();
					}
					final int w = out.write(buf);
					written += w;
					yield = (w == 0);
				}
				ret += written;
			}
		}
	}
}