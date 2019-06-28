/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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