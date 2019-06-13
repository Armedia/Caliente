package com.armedia.caliente.content;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.BinaryMemoryBuffer;
import com.armedia.commons.utilities.DigestInputStream;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;

public class TestDataGenerator extends BaseShareableLockable {

	private class RandomStream extends InputStream {
		private int size;

		private RandomStream(int size) {
			this.size = Math.max(0, size);
		}

		@Override
		public int read() throws IOException {
			if (this.size <= 0) { return -1; }
			this.size--;
			return TestDataGenerator.this.random.nextInt(255);
		}
	}

	private final Logger log = LoggerFactory.getLogger("console");

	private final Random random = new Random(System.nanoTime());
	private final ConcurrentMap<Integer, Pair<BinaryMemoryBuffer, String>> streams = new ConcurrentHashMap<>();
	private final int count;
	private final Function<Integer, Integer> sizeCalculator;

	public TestDataGenerator(int count) {
		this(count, null);
	}

	public TestDataGenerator(int count, Function<Integer, Integer> sizeCalculator) {
		this.count = Tools.ensureBetween(1, count, Integer.MAX_VALUE);
		if (sizeCalculator == null) {
			sizeCalculator = (i) -> ((i + 1) * 2048);
		}
		this.sizeCalculator = sizeCalculator;
	}

	public void reset() {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.streams.clear();
			for (long index = 0; index < this.count; index++) {
				getTestData(index);
			}
		}
	}

	public Pair<BinaryMemoryBuffer, String> getTestData(long count) {
		try (SharedAutoLock lock = autoSharedLock()) {
			final int index = (int) (count % this.count);
			final int streamSize = this.sizeCalculator.apply(index);
			return ConcurrentUtils.createIfAbsentUnchecked(this.streams, index, () -> {
				this.log.info("Rendering stream # {} of {} bytes ...", index, streamSize);
				try (InputStream in = new RandomStream(streamSize)) {
					try (DigestInputStream din = new DigestInputStream(in, DigestUtils.getSha256Digest())) {
						BinaryMemoryBuffer buf = new BinaryMemoryBuffer();
						try {
							IOUtils.copy(din, buf);
						} finally {
							buf.close();
						}
						String hash = Hex.encodeHexString(din.collectHash().getRight());
						this.log.info("Rendered stream # {} of {} bytes (hash = {})", index, streamSize, hash);
						return Pair.of(buf, hash);
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}
	}

	public InputStream getInputStream(long count) {
		return getTestData(count).getLeft().getInputStream();
	}

	public String getHash(long count) {
		return getTestData(count).getRight();
	}
}