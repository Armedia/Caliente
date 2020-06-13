package com.armedia.caliente.engine.local.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods for concatenating streams. Taken from <a href=
 * "https://github.com/TechEmpower/misc/blob/master/concat/src/main/java/rnd/StreamConcatenation.java">here</a>.
 *
 * @author Michael Hixson
 */
public final class StreamConcatenation {
	private StreamConcatenation() {
		throw new AssertionError("This class cannot be instantiated");
	}

	/**
	 * @see #concat(Collection)
	 */
	@SafeVarargs
	public static <T> Stream<T> concat(Stream<T>... streams) {
		return StreamConcatenation.concat(Arrays.asList(streams));
	}

	/**
	 * Creates a lazily concatenated stream whose elements are the elements of each of the input
	 * streams. In other words, the returned stream contains all the elements of the first input
	 * stream followed by all the elements of the second input stream, and so on.
	 *
	 * <p>
	 * Although this method does not eagerly consume the elements of the input streams, this method
	 * is a terminal operation for the input streams.
	 *
	 * <p>
	 * The returned stream is parallel if any of the input streams is parallel.
	 *
	 * <p>
	 * When the returned stream is closed, the close handlers for all the input streams are invoked.
	 * If one of those handlers throws an exception, that exception will be rethrown after the
	 * remaining handlers are invoked. If the remaining handlers throw exceptions, those exceptions
	 * are added as suppressed exceptions of the first.
	 *
	 * <p>
	 * If the argument array or any of the input streams are modified after being passed to this
	 * method, the behavior of this method is undefined.
	 *
	 * @param <T>
	 *            the type of stream elements
	 * @param streams
	 *            the streams to be concatenated
	 * @return the concatenation of the input streams
	 * @throws NullPointerException
	 *             if the argument array or any of the input streams are {@code null}
	 */
	public static <T> Stream<T> concat(Collection<Stream<T>> streams) {
		return StreamConcatenation.concatInternal(StreamConcatenation.toList(streams), Stream::spliterator,
			ConcatSpliterator.OfRef::new, StreamSupport::stream, Stream::empty);
	}

	/**
	 * @see #concatInt(Collection)
	 */
	public static IntStream concatInt(IntStream... streams) {
		return StreamConcatenation.concatInt(Arrays.asList(streams));
	}

	/**
	 * Creates a lazily concatenated stream whose elements are the elements of each of the input
	 * streams. In other words, the returned stream contains all the elements of the first input
	 * stream followed by all the elements of the second input stream, and so on.
	 *
	 * <p>
	 * Although this method does not eagerly consume the elements of the input streams, this method
	 * is a terminal operation for the input streams.
	 *
	 * <p>
	 * The returned stream is parallel if any of the input streams is parallel.
	 *
	 * <p>
	 * When the returned stream is closed, the close handlers for all the input streams are invoked.
	 * If one of those handlers throws an exception, that exception will be rethrown after the
	 * remaining handlers are invoked. If the remaining handlers throw exceptions, those exceptions
	 * are added as suppressed exceptions of the first.
	 *
	 * <p>
	 * If the argument array or any of the input streams are modified after being passed to this
	 * method, the behavior of this method is undefined.
	 *
	 * @param streams
	 *            the streams to be concatenated
	 * @return the concatenation of the input streams
	 * @throws NullPointerException
	 *             if the argument array or any of the input streams are {@code null}
	 */
	public static IntStream concatInt(Collection<IntStream> streams) {
		return StreamConcatenation.concatInternal(StreamConcatenation.toList(streams), IntStream::spliterator,
			ConcatSpliterator.OfInt::new, StreamSupport::intStream, IntStream::empty);
	}

	/**
	 * @see #concatLong(Collection)
	 */
	public static LongStream concatLong(LongStream... streams) {
		return StreamConcatenation.concatLong(Arrays.asList(streams));
	}

	/**
	 * Creates a lazily concatenated stream whose elements are the elements of each of the input
	 * streams. In other words, the returned stream contains all the elements of the first input
	 * stream followed by all the elements of the second input stream, and so on.
	 *
	 * <p>
	 * Although this method does not eagerly consume the elements of the input streams, this method
	 * is a terminal operation for the input streams.
	 *
	 * <p>
	 * The returned stream is parallel if any of the input streams is parallel.
	 *
	 * <p>
	 * When the returned stream is closed, the close handlers for all the input streams are invoked.
	 * If one of those handlers throws an exception, that exception will be rethrown after the
	 * remaining handlers are invoked. If the remaining handlers throw exceptions, those exceptions
	 * are added as suppressed exceptions of the first.
	 *
	 * <p>
	 * If the argument array or any of the input streams are modified after being passed to this
	 * method, the behavior of this method is undefined.
	 *
	 * @param streams
	 *            the streams to be concatenated
	 * @return the concatenation of the input streams
	 * @throws NullPointerException
	 *             if the argument array or any of the input streams are {@code null}
	 */
	public static LongStream concatLong(Collection<LongStream> streams) {
		return StreamConcatenation.concatInternal(StreamConcatenation.toList(streams), LongStream::spliterator,
			ConcatSpliterator.OfLong::new, StreamSupport::longStream, LongStream::empty);
	}

	/**
	 * @see #concatDouble(Collection)
	 */
	public static DoubleStream concatDouble(DoubleStream... streams) {
		return StreamConcatenation.concatDouble(Arrays.asList(streams));
	}

	/**
	 * Creates a lazily concatenated stream whose elements are the elements of each of the input
	 * streams. In other words, the returned stream contains all the elements of the first input
	 * stream followed by all the elements of the second input stream, and so on.
	 *
	 * <p>
	 * Although this method does not eagerly consume the elements of the input streams, this method
	 * is a terminal operation for the input streams.
	 *
	 * <p>
	 * The returned stream is parallel if any of the input streams is parallel.
	 *
	 * <p>
	 * When the returned stream is closed, the close handlers for all the input streams are invoked.
	 * If one of those handlers throws an exception, that exception will be rethrown after the
	 * remaining handlers are invoked. If the remaining handlers throw exceptions, those exceptions
	 * are added as suppressed exceptions of the first.
	 *
	 * <p>
	 * If the argument array or any of the input streams are modified after being passed to this
	 * method, the behavior of this method is undefined.
	 *
	 * @param streams
	 *            the streams to be concatenated
	 * @return the concatenation of the input streams
	 * @throws NullPointerException
	 *             if the argument array or any of the input streams are {@code null}
	 */
	public static DoubleStream concatDouble(Collection<DoubleStream> streams) {
		return StreamConcatenation.concatInternal(StreamConcatenation.toList(streams), DoubleStream::spliterator,
			ConcatSpliterator.OfDouble::new, StreamSupport::doubleStream, DoubleStream::empty);
	}

	private static <T, SPLITERATOR extends Spliterator<T>, STREAM extends BaseStream<T, STREAM>> List<STREAM> toList(
		Collection<STREAM> c) {
		if (c == null) { return Collections.emptyList(); }
		List<STREAM> l = new ArrayList<>(c);
		l.removeIf(Objects::isNull);
		return l;
	}

	// The generics and function objects are ugly, but this method lets us reuse
	// the same logic in all the public concat(...) methods.
	private static <T, SPLITERATOR extends Spliterator<T>, STREAM extends BaseStream<T, STREAM>> STREAM concatInternal(
		List<STREAM> streams, //
		Function<STREAM, SPLITERATOR> spliteratorFunction, //
		Function<List<SPLITERATOR>, SPLITERATOR> concatFunction, //
		BiFunction<SPLITERATOR, Boolean, STREAM> streamFunction, //
		Supplier<STREAM> empty) {

		List<SPLITERATOR> spliterators = new ArrayList<>(streams.size());
		boolean parallel = false;
		for (STREAM inStream : streams) {
			SPLITERATOR inSpliterator = spliteratorFunction.apply(inStream);
			spliterators.add(inSpliterator);
			parallel = parallel || inStream.isParallel();
		}
		SPLITERATOR outSpliterator = concatFunction.apply(spliterators);
		STREAM outStream = streamFunction.apply(outSpliterator, parallel);
		return outStream.onClose(new ComposedClose(streams));
	}

	abstract static class ConcatSpliterator<T, SPLITERATOR extends Spliterator<T>> implements Spliterator<T> {

		final List<SPLITERATOR> spliterators;
		int low; // increases only after trySplit()
		int cursor; // increases after iteration or trySplit()
		final int high;

		ConcatSpliterator(List<SPLITERATOR> spliterators, int fromIndex, int toIndex) {
			this.spliterators = spliterators;
			this.low = this.cursor = fromIndex;
			this.high = toIndex;
		}

		// Having these two abstract methods let us reuse the same trySplit()
		// implementation in all subclasses.

		// invokes spliterator.trySplit() on the argument
		abstract SPLITERATOR invokeTrySplit(SPLITERATOR spliterator);

		// invokes our constructor with the same array but modified bounds
		abstract SPLITERATOR slice(int fromIndex, int toIndex);

		@Override
		public int characteristics() {
			int i = this.low; // ignore the cursor; iteration can't affect characteristics
			if (i >= this.high) {
				// TODO(perf): This may report fewer characteristics than it should.
				return Spliterators.emptySpliterator().characteristics();
			}
			if (i == (this.high - 1)) {
				// note for getComparator(): this is the only time we might be SORTED
				return this.spliterators.get(i).characteristics();
			}
			//
			// DISTINCT and SORTED are *not* safe to inherit. Imagine our input
			// spliterators each contain the elements [1, 2] (distinct and sorted) and
			// so we are [1, 2, 1, 2] (neither distinct nor sorted).
			//
			// SIZED and SUBSIZED might be safe to inherit, but we have to account for
			// arithmetic overflow. If we're unable to produce a correct sum in
			// estimateSize() due to overflow, we must not report SIZED (or SUBSIZED).
			//
			// ORDERED, NONNULL, IMMUTABLE, and CONCURRENT are safe to inherit.
			//
			// We assume that all other characteristics not listed here (that do not
			// exist yet at the time of this writing) are *not* safe to inherit.
			// False negatives generally result in degraded performance, while false
			// positives generally result in incorrect behavior.
			//
			int characteristics = Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL
				| Spliterator.IMMUTABLE | Spliterator.CONCURRENT;
			long size = 0;
			for (SPLITERATOR s : this.spliterators.subList(this.low, this.high)) {
				characteristics &= s.characteristics();
				if ((characteristics & Spliterator.SIZED) == Spliterator.SIZED) {
					size += s.estimateSize();
					if (size < 0) { // overflow
						characteristics &= ~(Spliterator.SIZED | Spliterator.SUBSIZED);
					}
				}
			}
			return characteristics;
		}

		@Override
		public long estimateSize() {
			long size = 0;
			for (SPLITERATOR s : this.spliterators.subList(this.cursor, this.high)) {
				size += s.estimateSize();
				if (size < 0) { return Long.MAX_VALUE; }
			}
			return size;
		}

		@Override
		public void forEachRemaining(Consumer<? super T> action) {
			Objects.requireNonNull(action);
			for (SPLITERATOR s : this.spliterators.subList(this.cursor, this.high)) {
				s.forEachRemaining(action);
			}
			this.cursor = this.high;
		}

		@Override
		public Comparator<? super T> getComparator() {
			int i = this.low; // like characteristics()
			if (i == (this.high - 1)) {
				// this is the only time we might be SORTED; see characteristics()
				return this.spliterators.get(i).getComparator();
			}
			throw new IllegalStateException();
		}

		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			Objects.requireNonNull(action);
			if (this.cursor < this.high) {
				for (SPLITERATOR s : this.spliterators.subList(this.cursor, this.high)) {
					if (s.tryAdvance(action)) {
						// Mark the current position
						return true;
					}
					this.cursor++;
				}
				this.cursor = this.high;
			}
			return false;
		}

		@Override
		public SPLITERATOR trySplit() {
			//
			// TODO(perf): Should we split differently when we're SIZED?
			//
			// 1) Rather than splitting our *spliterators* in half, we could try to
			// split our *elements* in half.
			//
			// 2) We could refuse to split if our total element count is lower than
			// some threshold.
			//
			int i = this.cursor;
			if (i >= this.high) { return null; }
			if (i == (this.high - 1)) { return invokeTrySplit(this.spliterators.get(i)); }
			int mid = (i + this.high) >>> 1;
			this.low = this.cursor = mid;
			if (mid == (i + 1)) { return this.spliterators.get(i); }
			return slice(i, mid);
		}

		static final class OfRef<T> extends ConcatSpliterator<T, Spliterator<T>> {

			OfRef(List<Spliterator<T>> spliterators) {
				super(spliterators, 0, spliterators.size());
			}

			OfRef(List<Spliterator<T>> spliterators, int fromIndex, int toIndex) {
				super(spliterators, fromIndex, toIndex);
			}

			@Override
			Spliterator<T> invokeTrySplit(Spliterator<T> spliterator) {
				return spliterator.trySplit();
			}

			@Override
			Spliterator<T> slice(int fromIndex, int toIndex) {
				return new ConcatSpliterator.OfRef<>(this.spliterators, fromIndex, toIndex);
			}
		}

		abstract static class OfPrimitive<VALUE, CONSUMER, SPLITERATOR extends Spliterator.OfPrimitive<VALUE, CONSUMER, SPLITERATOR>>
			extends ConcatSpliterator<VALUE, SPLITERATOR>
			implements Spliterator.OfPrimitive<VALUE, CONSUMER, SPLITERATOR> {

			OfPrimitive(List<SPLITERATOR> spliterators, int fromIndex, int toIndex) {
				super(spliterators, fromIndex, toIndex);
			}

			@Override
			public void forEachRemaining(CONSUMER action) {
				Objects.requireNonNull(action);
				for (SPLITERATOR s : this.spliterators.subList(this.cursor, this.high)) {
					s.forEachRemaining(action);
					this.cursor++;
				}
			}

			@Override
			public boolean tryAdvance(CONSUMER action) {
				Objects.requireNonNull(action);
				int i = this.cursor;
				if (i < this.high) {
					do {
						if (this.spliterators.get(i).tryAdvance(action)) {
							this.cursor = i;
							return true;
						}
					} while (++i < this.high);
					this.cursor = this.high;
				}
				return false;
			}
		}

		static final class OfInt extends ConcatSpliterator.OfPrimitive<Integer, IntConsumer, Spliterator.OfInt>
			implements Spliterator.OfInt {

			OfInt(List<Spliterator.OfInt> spliterators) {
				super(spliterators, 0, spliterators.size());
			}

			OfInt(List<Spliterator.OfInt> spliterators, int fromIndex, int toIndex) {
				super(spliterators, fromIndex, toIndex);
			}

			@Override
			Spliterator.OfInt invokeTrySplit(Spliterator.OfInt spliterator) {
				return spliterator.trySplit();
			}

			@Override
			Spliterator.OfInt slice(int fromIndex, int toIndex) {
				return new ConcatSpliterator.OfInt(this.spliterators, fromIndex, toIndex);
			}
		}

		static final class OfLong extends ConcatSpliterator.OfPrimitive<Long, LongConsumer, Spliterator.OfLong>
			implements Spliterator.OfLong {

			OfLong(List<Spliterator.OfLong> spliterators) {
				super(spliterators, 0, spliterators.size());
			}

			OfLong(List<Spliterator.OfLong> spliterators, int fromIndex, int toIndex) {
				super(spliterators, fromIndex, toIndex);
			}

			@Override
			Spliterator.OfLong invokeTrySplit(Spliterator.OfLong spliterator) {
				return spliterator.trySplit();
			}

			@Override
			Spliterator.OfLong slice(int fromIndex, int toIndex) {
				return new ConcatSpliterator.OfLong(this.spliterators, fromIndex, toIndex);
			}
		}

		static final class OfDouble extends ConcatSpliterator.OfPrimitive<Double, DoubleConsumer, Spliterator.OfDouble>
			implements Spliterator.OfDouble {

			OfDouble(List<Spliterator.OfDouble> spliterators) {
				super(spliterators, 0, spliterators.size());
			}

			OfDouble(List<Spliterator.OfDouble> spliterators, int fromIndex, int toIndex) {
				super(spliterators, fromIndex, toIndex);
			}

			@Override
			Spliterator.OfDouble invokeTrySplit(Spliterator.OfDouble spliterator) {
				return spliterator.trySplit();
			}

			@Override
			Spliterator.OfDouble slice(int fromIndex, int toIndex) {
				return new ConcatSpliterator.OfDouble(this.spliterators, fromIndex, toIndex);
			}
		}
	}

	static final class ComposedClose implements Runnable {
		final Collection<? extends BaseStream<?, ?>> streams;

		ComposedClose(Collection<? extends BaseStream<?, ?>> streams) {
			this.streams = streams;
		}

		@Override
		public void run() {
			Iterator<? extends BaseStream<?, ?>> it = this.streams.iterator();
			while (it.hasNext()) {
				try {
					it.next().close();
				} catch (Throwable thrown) {
					while (it.hasNext()) {
						try {
							it.next().close();
						} catch (Throwable suppressed) {
							thrown.addSuppressed(suppressed);
						}
					}
					throw thrown;
				}
			}
		}
	}
}