package com.armedia.caliente.engine.local.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.SplittableRandom;
import java.util.TreeSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StreamConcatenation}.
 *
 * @author Michael Hixson
 */
public final class StreamConcatenationTest {

	static Stream<String> refStream(int numberOfStreams, int elementsPerStream, boolean sorted) {
		@SuppressWarnings("unchecked")
		Stream<String>[] streams = (Stream<String>[]) new Stream<?>[numberOfStreams];
		for (int i = 0; i < numberOfStreams; i++) {
			Collection<String> elements;
			if (sorted) {
				elements = new TreeSet<>();
			} else {
				elements = new ArrayList<>();
			}
			for (int j = 0; j < elementsPerStream; j++) {
				elements.add(i + "," + j);
			}
			streams[i] = elements.stream();
		}
		return StreamConcatenation.concat(streams);
	}

	static IntStream intStream(int numberOfStreams, int elementsPerStream, boolean sorted) {
		IntStream[] streams = new IntStream[numberOfStreams];
		for (int i = 0; i < numberOfStreams; i++) {
			int[] elements = new int[elementsPerStream];
			for (int j = 0; j < elements.length; j++) {
				elements[j] = (i * elementsPerStream) + j;
			}
			IntStream stream = IntStream.of(elements);
			if (sorted) {
				stream = stream.distinct().sorted();
			}
			streams[i] = stream;
		}
		return StreamConcatenation.concatInt(streams);
	}

	static LongStream longStream(int numberOfStreams, int elementsPerStream, boolean sorted) {
		LongStream[] streams = new LongStream[numberOfStreams];
		for (int i = 0; i < numberOfStreams; i++) {
			long[] elements = new long[elementsPerStream];
			for (int j = 0; j < elements.length; j++) {
				elements[j] = (i * elementsPerStream) + j;
			}
			LongStream stream = LongStream.of(elements);
			if (sorted) {
				stream = stream.distinct().sorted();
			}
			streams[i] = stream;
		}
		return StreamConcatenation.concatLong(streams);
	}

	static DoubleStream doubleStream(int numberOfStreams, int elementsPerStream, boolean sorted) {
		DoubleStream[] streams = new DoubleStream[numberOfStreams];
		for (int i = 0; i < numberOfStreams; i++) {
			double[] elements = new double[elementsPerStream];
			for (int j = 0; j < elements.length; j++) {
				elements[j] = (i * elementsPerStream) + j;
			}
			DoubleStream stream = DoubleStream.of(elements);
			if (sorted) {
				stream = stream.distinct().sorted();
			}
			streams[i] = stream;
		}
		return StreamConcatenation.concatDouble(streams);
	}

	/**
	 * Tests that {@link Spliterator#characteristics()} returns the characteristics of the empty
	 * spliterator for a concatenation of zero input streams.
	 */
	@Test
	public void testSpliteratorCharacteristicsNoInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(0, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(0, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(0, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(0, 0, false);
		Assertions.assertEquals(Spliterators.emptySpliterator().characteristics(),
			stream.spliterator().characteristics(), "ref");
		Assertions.assertEquals(Spliterators.emptySpliterator().characteristics(),
			intStream.spliterator().characteristics(), "int");
		Assertions.assertEquals(Spliterators.emptySpliterator().characteristics(),
			longStream.spliterator().characteristics(), "long");
		Assertions.assertEquals(Spliterators.emptySpliterator().characteristics(),
			doubleStream.spliterator().characteristics(), "double");
	}

	/**
	 * Tests that {@link Spliterator#estimateSize()} returns {@code 0} for a concatenation of zero
	 * input streams.
	 */
	@Test
	public void testSpliteratorEstimateSizeNoInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(0, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(0, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(0, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(0, 0, false);
		Assertions.assertEquals(0, stream.spliterator().estimateSize(), "ref");
		Assertions.assertEquals(0, intStream.spliterator().estimateSize(), "int");
		Assertions.assertEquals(0, longStream.spliterator().estimateSize(), "long");
		Assertions.assertEquals(0, doubleStream.spliterator().estimateSize(), "double");
	}

	/**
	 * Tests that {@link Spliterator#forEachRemaining(Consumer)} does not pass any elements to the
	 * consumer for a concatenation of zero input streams.
	 */
	@Test
	public void testSpliteratorForEachRemainingNoInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(0, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(0, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(0, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(0, 0, false);
		stream.spliterator().forEachRemaining(x -> Assertions.fail("ref"));
		intStream.spliterator().forEachRemaining((int x) -> Assertions.fail("int"));
		longStream.spliterator().forEachRemaining((long x) -> Assertions.fail("long"));
		doubleStream.spliterator().forEachRemaining((double x) -> Assertions.fail("double"));
	}

	/**
	 * Tests that {@link Spliterator#tryAdvance(Consumer)} does not pass any elements to the
	 * consumer for a concatenation of zero input streams.
	 */
	@Test
	public void testSpliteratorTryAdvanceNoInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(0, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(0, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(0, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(0, 0, false);
		boolean advanced;
		advanced = stream.spliterator().tryAdvance(x -> Assertions.fail("ref element"));
		Assertions.assertFalse(advanced, "ref advanced");
		advanced = intStream.spliterator().tryAdvance((int x) -> Assertions.fail("int element"));
		Assertions.assertFalse(advanced, "int advanced");
		advanced = longStream.spliterator().tryAdvance((long x) -> Assertions.fail("long element"));
		Assertions.assertFalse(advanced, "long advanced");
		advanced = doubleStream.spliterator().tryAdvance((double x) -> Assertions.fail("double element"));
		Assertions.assertFalse(advanced, "double advanced");
	}

	/**
	 * Tests that {@link Spliterator#trySplit()} returns {@code null} for a concatenation of zero
	 * input streams.
	 */
	@Test
	public void testSpliteratorTrySplitNoInputsIsNotSplittable() {
		Stream<String> stream = StreamConcatenationTest.refStream(0, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(0, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(0, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(0, 0, false);
		Assertions.assertNull(stream.spliterator().trySplit(), "ref");
		Assertions.assertNull(intStream.spliterator().trySplit(), "int");
		Assertions.assertNull(longStream.spliterator().trySplit(), "long");
		Assertions.assertNull(doubleStream.spliterator().trySplit(), "double");
	}

	/**
	 * Tests that {@link Spliterator#trySplit()} does not return {@code null} for a concatenation of
	 * multiple input streams, even when the input streams are all empty.
	 */
	@Test
	public void testSpliteratorTrySplitMultipleInputsIsSplittable() {
		Stream<String> stream = StreamConcatenationTest.refStream(3, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(3, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(3, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(3, 0, false);
		Assertions.assertNotNull(stream.spliterator().trySplit(), "ref 0");
		Assertions.assertNotNull(intStream.spliterator().trySplit(), "int 0");
		Assertions.assertNotNull(longStream.spliterator().trySplit(), "long 0");
		Assertions.assertNotNull(doubleStream.spliterator().trySplit(), "double 0");
		stream = StreamConcatenationTest.refStream(3, 1, false);
		intStream = StreamConcatenationTest.intStream(3, 1, false);
		longStream = StreamConcatenationTest.longStream(3, 1, false);
		doubleStream = StreamConcatenationTest.doubleStream(3, 1, false);
		Assertions.assertNotNull(stream.spliterator().trySplit(), "ref 1");
		Assertions.assertNotNull(intStream.spliterator().trySplit(), "int 1");
		Assertions.assertNotNull(longStream.spliterator().trySplit(), "long 1");
		Assertions.assertNotNull(doubleStream.spliterator().trySplit(), "double 1");
		stream = StreamConcatenationTest.refStream(3, 3, false);
		intStream = StreamConcatenationTest.intStream(3, 3, false);
		longStream = StreamConcatenationTest.longStream(3, 3, false);
		doubleStream = StreamConcatenationTest.doubleStream(3, 3, false);
		Assertions.assertNotNull(stream.spliterator().trySplit(), "ref many");
		Assertions.assertNotNull(intStream.spliterator().trySplit(), "int many");
		Assertions.assertNotNull(longStream.spliterator().trySplit(), "long many");
		Assertions.assertNotNull(doubleStream.spliterator().trySplit(), "double many");
	}

	/**
	 * Tests that {@link Spliterator#estimateSize()} returns {@code 0} for a concatenation of sized,
	 * empty input streams.
	 */
	@Test
	public void testSpliteratorEstimateSizeEmptyInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 0, false);
		Assertions.assertEquals(0, stream.spliterator().estimateSize(), "ref 1");
		Assertions.assertEquals(0, intStream.spliterator().estimateSize(), "int 1");
		Assertions.assertEquals(0, longStream.spliterator().estimateSize(), "long 1");
		Assertions.assertEquals(0, doubleStream.spliterator().estimateSize(), "double 1");
		stream = StreamConcatenationTest.refStream(3, 0, false);
		intStream = StreamConcatenationTest.intStream(3, 0, false);
		longStream = StreamConcatenationTest.longStream(3, 0, false);
		doubleStream = StreamConcatenationTest.doubleStream(3, 0, false);
		Assertions.assertEquals(0, stream.spliterator().estimateSize(), "ref many");
		Assertions.assertEquals(0, intStream.spliterator().estimateSize(), "int many");
		Assertions.assertEquals(0, longStream.spliterator().estimateSize(), "long many");
		Assertions.assertEquals(0, doubleStream.spliterator().estimateSize(), "double many");
	}

	/**
	 * Tests that {@link Spliterator#forEachRemaining(Consumer)} for a concatenation of empty input
	 * streams will not invoke the provided action.
	 */
	@Test
	public void testSpliteratorForEachRemainingEmptyInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 0, false);
		stream.spliterator().forEachRemaining(x -> Assertions.fail("ref 1"));
		intStream.spliterator().forEachRemaining((int x) -> Assertions.fail("int 1"));
		longStream.spliterator().forEachRemaining((long x) -> Assertions.fail("long 1"));
		doubleStream.spliterator().forEachRemaining((double x) -> Assertions.fail("double 1"));
		stream = StreamConcatenationTest.refStream(3, 0, false);
		intStream = StreamConcatenationTest.intStream(3, 0, false);
		longStream = StreamConcatenationTest.longStream(3, 0, false);
		doubleStream = StreamConcatenationTest.doubleStream(3, 0, false);
		stream.spliterator().forEachRemaining(x -> Assertions.fail("ref many"));
		intStream.spliterator().forEachRemaining((int x) -> Assertions.fail("int many"));
		longStream.spliterator().forEachRemaining((long x) -> Assertions.fail("long many"));
		doubleStream.spliterator().forEachRemaining((double x) -> Assertions.fail("double many"));
	}

	/**
	 * Tests that {@link Spliterator#tryAdvance(Consumer)} for a concatenation of empty input
	 * streams will not invoke the provided action and will not report that it has advanced.
	 */
	@Test
	public void testSpliteratorTryAdvanceEmptyInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 0, false);
		boolean advanced;
		advanced = stream.spliterator().tryAdvance(x -> Assertions.fail("ref 1 element"));
		Assertions.assertFalse(advanced, "ref 1 advanced");
		advanced = intStream.spliterator().tryAdvance((int x) -> Assertions.fail("int 1 element"));
		Assertions.assertFalse(advanced, "int 1 advanced");
		advanced = longStream.spliterator().tryAdvance((long x) -> Assertions.fail("long 1 element"));
		Assertions.assertFalse(advanced, "long 1 advanced");
		advanced = doubleStream.spliterator().tryAdvance((double x) -> Assertions.fail("double 1 element"));
		Assertions.assertFalse(advanced, "double 1 advanced");
		stream = StreamConcatenationTest.refStream(3, 0, false);
		intStream = StreamConcatenationTest.intStream(3, 0, false);
		longStream = StreamConcatenationTest.longStream(3, 0, false);
		doubleStream = StreamConcatenationTest.doubleStream(3, 0, false);
		advanced = stream.spliterator().tryAdvance(x -> Assertions.fail("ref many element"));
		Assertions.assertFalse(advanced, "ref many advanced");
		advanced = intStream.spliterator().tryAdvance((int x) -> Assertions.fail("int many element"));
		Assertions.assertFalse(advanced, "int many advanced");
		advanced = longStream.spliterator().tryAdvance((long x) -> Assertions.fail("long many element"));
		Assertions.assertFalse(advanced, "long many advanced");
		advanced = doubleStream.spliterator().tryAdvance((double x) -> Assertions.fail("double many element"));
		Assertions.assertFalse(advanced, "double many advanced");
	}

	/**
	 * Tests that {@link Spliterator#estimateSize()} for a concatenation of sized, non-empty input
	 * streams reports the combined size of its inputs.
	 */
	@Test
	public void testSpliteratorEstimateSizeNonEmptyInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 1, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 1, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 1, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 1, false);
		Assertions.assertEquals(1, stream.spliterator().estimateSize(), "ref 1x1");
		Assertions.assertEquals(1, intStream.spliterator().estimateSize(), "int 1x1");
		Assertions.assertEquals(1, longStream.spliterator().estimateSize(), "long 1x1");
		Assertions.assertEquals(1, doubleStream.spliterator().estimateSize(), "double 1x1");
		stream = StreamConcatenationTest.refStream(1, 3, false);
		intStream = StreamConcatenationTest.intStream(1, 3, false);
		longStream = StreamConcatenationTest.longStream(1, 3, false);
		doubleStream = StreamConcatenationTest.doubleStream(1, 3, false);
		Assertions.assertEquals(3, stream.spliterator().estimateSize(), "ref 1xmany");
		Assertions.assertEquals(3, intStream.spliterator().estimateSize(), "int 1xmany");
		Assertions.assertEquals(3, longStream.spliterator().estimateSize(), "long 1xmany");
		Assertions.assertEquals(3, doubleStream.spliterator().estimateSize(), "double 1xmany");
		stream = StreamConcatenationTest.refStream(3, 1, false);
		intStream = StreamConcatenationTest.intStream(3, 1, false);
		longStream = StreamConcatenationTest.longStream(3, 1, false);
		doubleStream = StreamConcatenationTest.doubleStream(3, 1, false);
		Assertions.assertEquals(3, stream.spliterator().estimateSize(), "ref manyx1");
		Assertions.assertEquals(3, intStream.spliterator().estimateSize(), "int manyx1");
		Assertions.assertEquals(3, longStream.spliterator().estimateSize(), "long manyx1");
		Assertions.assertEquals(3, doubleStream.spliterator().estimateSize(), "double manyx1");
		stream = StreamConcatenationTest.refStream(3, 3, false);
		intStream = StreamConcatenationTest.intStream(3, 3, false);
		longStream = StreamConcatenationTest.longStream(3, 3, false);
		doubleStream = StreamConcatenationTest.doubleStream(3, 3, false);
		Assertions.assertEquals(9, stream.spliterator().estimateSize(), "ref manyxmany");
		Assertions.assertEquals(9, intStream.spliterator().estimateSize(), "int manyxmany");
		Assertions.assertEquals(9, longStream.spliterator().estimateSize(), "long manyxmany");
		Assertions.assertEquals(9, doubleStream.spliterator().estimateSize(), "double manyxmany");
	}

	/**
	 * Tests that {@link Spliterator#forEachRemaining(Consumer)} for a concatenation of one input
	 * stream with one element iterates through the expected element.
	 */
	@Test
	public void testSpliteratorForEachRemainingOneByOne() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 1, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 1, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 1, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 1, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		List<String> out = new ArrayList<>();
		List<Integer> intOut = new ArrayList<>();
		List<Long> longOut = new ArrayList<>();
		List<Double> doubleOut = new ArrayList<>();
		spliterator.forEachRemaining(out::add);
		intSpliterator.forEachRemaining((IntConsumer) intOut::add);
		longSpliterator.forEachRemaining((LongConsumer) longOut::add);
		doubleSpliterator.forEachRemaining((DoubleConsumer) doubleOut::add);
		Assertions.assertEquals(Collections.singletonList("0,0"), out, "ref contents");
		Assertions.assertEquals(Collections.singletonList(0), intOut, "int contents");
		Assertions.assertEquals(Collections.singletonList(0L), longOut, "long contents");
		Assertions.assertEquals(Collections.singletonList(0d), doubleOut, "double contents");
		spliterator.forEachRemaining(x -> Assertions.fail("ref element"));
		intSpliterator.forEachRemaining((int x) -> Assertions.fail("int element"));
		longSpliterator.forEachRemaining((long x) -> Assertions.fail("long element"));
		doubleSpliterator.forEachRemaining((double x) -> Assertions.fail("double element"));
	}

	/**
	 * Tests that {@link Spliterator#forEachRemaining(Consumer)} for a concatenation of one input
	 * stream with many elements iterates through the expected elements.
	 */
	@Test
	public void testSpliteratorForEachRemainingOneByMany() {
		Stream<String> stream = StreamConcatenationTest.refStream(3, 1, false);
		IntStream intStream = StreamConcatenationTest.intStream(3, 1, false);
		LongStream longStream = StreamConcatenationTest.longStream(3, 1, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(3, 1, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		List<String> out = new ArrayList<>();
		List<Integer> intOut = new ArrayList<>();
		List<Long> longOut = new ArrayList<>();
		List<Double> doubleOut = new ArrayList<>();
		spliterator.forEachRemaining(out::add);
		intSpliterator.forEachRemaining((IntConsumer) intOut::add);
		longSpliterator.forEachRemaining((LongConsumer) longOut::add);
		doubleSpliterator.forEachRemaining((DoubleConsumer) doubleOut::add);
		Assertions.assertEquals(Arrays.asList("0,0", "1,0", "2,0"), out, "ref contents");
		Assertions.assertEquals(Arrays.asList(0, 1, 2), intOut, "int contents");
		Assertions.assertEquals(Arrays.asList(0L, 1L, 2L), longOut, "long contents");
		Assertions.assertEquals(Arrays.asList(0d, 1d, 2d), doubleOut, "double contents");
		spliterator.forEachRemaining(x -> Assertions.fail("ref element"));
		intSpliterator.forEachRemaining((int x) -> Assertions.fail("int element"));
		longSpliterator.forEachRemaining((long x) -> Assertions.fail("long element"));
		doubleSpliterator.forEachRemaining((double x) -> Assertions.fail("double element"));
	}

	/**
	 * Tests that {@link Spliterator#forEachRemaining(Consumer)} for a concatenation of many input
	 * streams with one element each iterates through the expected elements.
	 */
	@Test
	public void testSpliteratorForEachRemainingManyByOne() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 3, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 3, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 3, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 3, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		List<String> out = new ArrayList<>();
		List<Integer> intOut = new ArrayList<>();
		List<Long> longOut = new ArrayList<>();
		List<Double> doubleOut = new ArrayList<>();
		spliterator.forEachRemaining(out::add);
		intSpliterator.forEachRemaining((IntConsumer) intOut::add);
		longSpliterator.forEachRemaining((LongConsumer) longOut::add);
		doubleSpliterator.forEachRemaining((DoubleConsumer) doubleOut::add);
		Assertions.assertEquals(Arrays.asList("0,0", "0,1", "0,2"), out, "ref contents");
		Assertions.assertEquals(Arrays.asList(0, 1, 2), intOut, "int contents");
		Assertions.assertEquals(Arrays.asList(0L, 1L, 2L), longOut, "long contents");
		Assertions.assertEquals(Arrays.asList(0d, 1d, 2d), doubleOut, "double contents");
		spliterator.forEachRemaining(x -> Assertions.fail("ref element"));
		intSpliterator.forEachRemaining((int x) -> Assertions.fail("int element"));
		longSpliterator.forEachRemaining((long x) -> Assertions.fail("long element"));
		doubleSpliterator.forEachRemaining((double x) -> Assertions.fail("double element"));
	}

	/**
	 * Tests that {@link Spliterator#forEachRemaining(Consumer)} for a concatenation of many input
	 * streams with many elements each iterates through the expected elements.
	 */
	@Test
	public void testSpliteratorForEachRemainingManyByMany() {
		Stream<String> stream = StreamConcatenationTest.refStream(3, 3, false);
		IntStream intStream = StreamConcatenationTest.intStream(3, 3, false);
		LongStream longStream = StreamConcatenationTest.longStream(3, 3, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(3, 3, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		List<String> out = new ArrayList<>();
		List<Integer> intOut = new ArrayList<>();
		List<Long> longOut = new ArrayList<>();
		List<Double> doubleOut = new ArrayList<>();
		spliterator.forEachRemaining(out::add);
		intSpliterator.forEachRemaining((IntConsumer) intOut::add);
		longSpliterator.forEachRemaining((LongConsumer) longOut::add);
		doubleSpliterator.forEachRemaining((DoubleConsumer) doubleOut::add);
		Assertions.assertEquals(Arrays.asList("0,0", "0,1", "0,2", "1,0", "1,1", "1,2", "2,0", "2,1", "2,2"), out,
			"ref contents");
		Assertions.assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8), intOut, "int contents");
		Assertions.assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), longOut, "long contents");
		Assertions.assertEquals(Arrays.asList(0d, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d), doubleOut, "double contents");
		spliterator.forEachRemaining(x -> Assertions.fail("ref element"));
		intSpliterator.forEachRemaining((int x) -> Assertions.fail("int element"));
		longSpliterator.forEachRemaining((long x) -> Assertions.fail("long element"));
		doubleSpliterator.forEachRemaining((double x) -> Assertions.fail("double element"));
	}

	/**
	 * Tests that {@link Spliterator#tryAdvance(Consumer)} for a concatenation of one input stream
	 * with one element iterates through the expected element.
	 */
	@Test
	public void testSpliteratorTryAdvanceOneByOne() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 1, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 1, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 1, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 1, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		LongAdder counter = new LongAdder();
		LongAdder intCounter = new LongAdder();
		LongAdder longCounter = new LongAdder();
		LongAdder doubleCounter = new LongAdder();
		boolean advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("0,0", x, "ref contents");
			counter.increment();
		});
		boolean intAdvanced = intSpliterator.tryAdvance((int x) -> {
			Assertions.assertEquals(0, x, "int contents");
			intCounter.increment();
		});
		boolean longAdvanced = longSpliterator.tryAdvance((long x) -> {
			Assertions.assertEquals(0L, x, "long contents");
			longCounter.increment();
		});
		boolean doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> {
			Assertions.assertEquals(0d, x, 0.1, "double contents");
			doubleCounter.increment();
		});
		Assertions.assertEquals(1, counter.sum(), "ref counter");
		Assertions.assertEquals(1, intCounter.sum(), "int counter");
		Assertions.assertEquals(1, longCounter.sum(), "long counter");
		Assertions.assertEquals(1, doubleCounter.sum(), "double counter");
		Assertions.assertTrue(advanced, "ref advanced first");
		Assertions.assertTrue(intAdvanced, "int advanced first");
		Assertions.assertTrue(longAdvanced, "long advanced first");
		Assertions.assertTrue(doubleAdvanced, "double advanced first");
		advanced = spliterator.tryAdvance(x -> Assertions.fail("ref element"));
		intAdvanced = intSpliterator.tryAdvance((int x) -> Assertions.fail("int element"));
		longAdvanced = longSpliterator.tryAdvance((long x) -> Assertions.fail("long element"));
		doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> Assertions.fail("double element"));
		Assertions.assertFalse(advanced, "ref advanced second");
		Assertions.assertFalse(intAdvanced, "int advanced second");
		Assertions.assertFalse(longAdvanced, "long advanced second");
		Assertions.assertFalse(doubleAdvanced, "double advanced second");
	}

	/**
	 * Tests that {@link Spliterator#tryAdvance(Consumer)} for a concatenation of one input stream
	 * with many elements iterates through the expected elements.
	 */
	@Test
	public void testSpliteratorTryAdvanceOneByMany() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 3, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 3, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 3, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 3, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		LongAdder counter = new LongAdder();
		LongAdder intCounter = new LongAdder();
		LongAdder longCounter = new LongAdder();
		LongAdder doubleCounter = new LongAdder();
		boolean advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("0,0", x, "ref contents 1");
			counter.increment();
		});
		boolean intAdvanced = intSpliterator.tryAdvance((int x) -> {
			Assertions.assertEquals(0, x, "int contents 1");
			intCounter.increment();
		});
		boolean longAdvanced = longSpliterator.tryAdvance((long x) -> {
			Assertions.assertEquals(0L, x, "long contents 1");
			longCounter.increment();
		});
		boolean doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> {
			Assertions.assertEquals(0d, x, 0.1, "double contents 1");
			doubleCounter.increment();
		});
		Assertions.assertEquals(1, counter.sum(), "ref counter 1");
		Assertions.assertEquals(1, intCounter.sum(), "int counter 1");
		Assertions.assertEquals(1, longCounter.sum(), "long counter 1");
		Assertions.assertEquals(1, doubleCounter.sum(), "double counter 1");
		Assertions.assertTrue(advanced, "ref advanced 1");
		Assertions.assertTrue(intAdvanced, "int advanced 1");
		Assertions.assertTrue(longAdvanced, "long advanced 1");
		Assertions.assertTrue(doubleAdvanced, "double advanced 1");
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("0,1", x, "ref contents 2");
			counter.increment();
		});
		intAdvanced = intSpliterator.tryAdvance((int x) -> {
			Assertions.assertEquals(1, x, "int contents 2");
			intCounter.increment();
		});
		longAdvanced = longSpliterator.tryAdvance((long x) -> {
			Assertions.assertEquals(1L, x, "long contents 2");
			longCounter.increment();
		});
		doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> {
			Assertions.assertEquals(1d, x, 0.1, "double contents 2");
			doubleCounter.increment();
		});
		Assertions.assertEquals(2, counter.sum(), "ref counter 2");
		Assertions.assertEquals(2, intCounter.sum(), "int counter 2");
		Assertions.assertEquals(2, longCounter.sum(), "long counter 2");
		Assertions.assertEquals(2, doubleCounter.sum(), "double counter 2");
		Assertions.assertTrue(advanced, "ref advanced 2");
		Assertions.assertTrue(intAdvanced, "int advanced 2");
		Assertions.assertTrue(longAdvanced, "long advanced 2");
		Assertions.assertTrue(doubleAdvanced, "double advanced 2");
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("0,2", x, "ref contents 3");
			counter.increment();
		});
		intAdvanced = intSpliterator.tryAdvance((int x) -> {
			Assertions.assertEquals(2, x, "int contents 3");
			intCounter.increment();
		});
		longAdvanced = longSpliterator.tryAdvance((long x) -> {
			Assertions.assertEquals(2L, x, "long contents 3");
			longCounter.increment();
		});
		doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> {
			Assertions.assertEquals(2d, x, 0.1, "double contents 3");
			doubleCounter.increment();
		});
		Assertions.assertEquals(3, counter.sum(), "ref counter 3");
		Assertions.assertEquals(3, intCounter.sum(), "int counter 3");
		Assertions.assertEquals(3, longCounter.sum(), "long counter 3");
		Assertions.assertEquals(3, doubleCounter.sum(), "double counter 3");
		Assertions.assertTrue(advanced, "ref advanced 3");
		Assertions.assertTrue(intAdvanced, "int advanced 3");
		Assertions.assertTrue(longAdvanced, "long advanced 3");
		Assertions.assertTrue(doubleAdvanced, "double advanced 3");
		advanced = spliterator.tryAdvance(x -> Assertions.fail("ref element"));
		intAdvanced = intSpliterator.tryAdvance((int x) -> Assertions.fail("int element"));
		longAdvanced = longSpliterator.tryAdvance((long x) -> Assertions.fail("long element"));
		doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> Assertions.fail("double element"));
		Assertions.assertFalse(advanced, "ref advanced 4");
		Assertions.assertFalse(intAdvanced, "int advanced 4");
		Assertions.assertFalse(longAdvanced, "long advanced 4");
		Assertions.assertFalse(doubleAdvanced, "double advanced 4");
	}

	/**
	 * Tests that {@link Spliterator#tryAdvance(Consumer)} for a concatenation of many input streams
	 * with one element each iterates through the expected elements.
	 */
	@Test
	public void testSpliteratorTryAdvanceManyByOne() {
		Stream<String> stream = StreamConcatenationTest.refStream(3, 1, false);
		IntStream intStream = StreamConcatenationTest.intStream(3, 1, false);
		LongStream longStream = StreamConcatenationTest.longStream(3, 1, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(3, 1, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		LongAdder counter = new LongAdder();
		LongAdder intCounter = new LongAdder();
		LongAdder longCounter = new LongAdder();
		LongAdder doubleCounter = new LongAdder();
		boolean advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("0,0", x, "ref contents 1");
			counter.increment();
		});
		boolean intAdvanced = intSpliterator.tryAdvance((int x) -> {
			Assertions.assertEquals(0, x, "int contents 1");
			intCounter.increment();
		});
		boolean longAdvanced = longSpliterator.tryAdvance((long x) -> {
			Assertions.assertEquals(0L, x, "long contents 1");
			longCounter.increment();
		});
		boolean doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> {
			Assertions.assertEquals(0d, x, 0.1, "double contents 1");
			doubleCounter.increment();
		});
		Assertions.assertEquals(1, counter.sum(), "ref counter 1");
		Assertions.assertEquals(1, intCounter.sum(), "int counter 1");
		Assertions.assertEquals(1, longCounter.sum(), "long counter 1");
		Assertions.assertEquals(1, doubleCounter.sum(), "double counter 1");
		Assertions.assertTrue(advanced, "ref advanced 1");
		Assertions.assertTrue(intAdvanced, "int advanced 1");
		Assertions.assertTrue(longAdvanced, "long advanced 1");
		Assertions.assertTrue(doubleAdvanced, "double advanced 1");
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("1,0", x, "ref contents 2");
			counter.increment();
		});
		intAdvanced = intSpliterator.tryAdvance((int x) -> {
			Assertions.assertEquals(1, x, "int contents 2");
			intCounter.increment();
		});
		longAdvanced = longSpliterator.tryAdvance((long x) -> {
			Assertions.assertEquals(1L, x, "long contents 2");
			longCounter.increment();
		});
		doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> {
			Assertions.assertEquals(1d, x, 0.1, "double contents 2");
			doubleCounter.increment();
		});
		Assertions.assertEquals(2, counter.sum(), "ref counter 2");
		Assertions.assertEquals(2, intCounter.sum(), "int counter 2");
		Assertions.assertEquals(2, longCounter.sum(), "long counter 2");
		Assertions.assertEquals(2, doubleCounter.sum(), "double counter 2");
		Assertions.assertTrue(advanced, "ref advanced 2");
		Assertions.assertTrue(intAdvanced, "int advanced 2");
		Assertions.assertTrue(longAdvanced, "long advanced 2");
		Assertions.assertTrue(doubleAdvanced, "double advanced 2");
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("2,0", x, "ref contents 3");
			counter.increment();
		});
		intAdvanced = intSpliterator.tryAdvance((int x) -> {
			Assertions.assertEquals(2, x, "int contents 3");
			intCounter.increment();
		});
		longAdvanced = longSpliterator.tryAdvance((long x) -> {
			Assertions.assertEquals(2L, x, "long contents 3");
			longCounter.increment();
		});
		doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> {
			Assertions.assertEquals(2d, x, 0.1, "double contents 3");
			doubleCounter.increment();
		});
		Assertions.assertEquals(3, counter.sum(), "ref counter 3");
		Assertions.assertEquals(3, intCounter.sum(), "int counter 3");
		Assertions.assertEquals(3, longCounter.sum(), "long counter 3");
		Assertions.assertEquals(3, doubleCounter.sum(), "double counter 3");
		Assertions.assertTrue(advanced, "ref advanced 3");
		Assertions.assertTrue(intAdvanced, "int advanced 3");
		Assertions.assertTrue(longAdvanced, "long advanced 3");
		Assertions.assertTrue(doubleAdvanced, "double advanced 3");
		advanced = spliterator.tryAdvance(x -> Assertions.fail("ref element"));
		intAdvanced = intSpliterator.tryAdvance((int x) -> Assertions.fail("int element"));
		longAdvanced = longSpliterator.tryAdvance((long x) -> Assertions.fail("long element"));
		doubleAdvanced = doubleSpliterator.tryAdvance((double x) -> Assertions.fail("double element"));
		Assertions.assertFalse(advanced, "ref advanced 4");
		Assertions.assertFalse(intAdvanced, "int advanced 4");
		Assertions.assertFalse(longAdvanced, "long advanced 4");
		Assertions.assertFalse(doubleAdvanced, "double advanced 4");
	}

	/**
	 * Tests that {@link Spliterator#tryAdvance(Consumer)} for a concatenation of many input streams
	 * with many elements each iterates through the expected elements.
	 */
	@Test
	public void testSpliteratorTryAdvanceManyByMany() {
		// TODO: Add assertions for primitive streams.
		Stream<String> stream = StreamConcatenationTest.refStream(3, 3, false);
		Spliterator<String> spliterator = stream.spliterator();
		LongAdder counter = new LongAdder();
		boolean advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("0,0", x);
			counter.increment();
		});
		Assertions.assertEquals(1, counter.sum());
		Assertions.assertTrue(advanced);
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("0,1", x);
			counter.increment();
		});
		Assertions.assertEquals(2, counter.sum());
		Assertions.assertTrue(advanced);
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("0,2", x);
			counter.increment();
		});
		Assertions.assertEquals(3, counter.sum());
		Assertions.assertTrue(advanced);
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("1,0", x);
			counter.increment();
		});
		Assertions.assertEquals(4, counter.sum());
		Assertions.assertTrue(advanced);
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("1,1", x);
			counter.increment();
		});
		Assertions.assertEquals(5, counter.sum());
		Assertions.assertTrue(advanced);
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("1,2", x);
			counter.increment();
		});
		Assertions.assertEquals(6, counter.sum());
		Assertions.assertTrue(advanced);
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("2,0", x);
			counter.increment();
		});
		Assertions.assertEquals(7, counter.sum());
		Assertions.assertTrue(advanced);
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("2,1", x);
			counter.increment();
		});
		Assertions.assertEquals(8, counter.sum());
		Assertions.assertTrue(advanced);
		advanced = spliterator.tryAdvance(x -> {
			Assertions.assertEquals("2,2", x);
			counter.increment();
		});
		Assertions.assertEquals(9, counter.sum());
		Assertions.assertTrue(advanced);
		advanced = spliterator.tryAdvance(x -> Assertions.fail());
		Assertions.assertFalse(advanced);
	}

	/**
	 * Test that {@link Spliterator#estimateSize()} reports the combined characteristics of the
	 * spliterators of the input streams for a concatenation of multiple input streams, each with
	 * multiple elements.
	 */
	@Test
	public void testSpliteratorCharacteristicsMultipleInputsMultipleElements() {
		// TODO: Add assertions for primitive streams.
		// TODO: Make one of the spliterators sorted, confirm the result has the
		// cross-section of characteristics of the inputs.
		Stream<String> stream = StreamConcatenationTest.refStream(3, 3, false);
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
			stream.spliterator().characteristics());
	}

	/**
	 * Test that {@link Spliterator#estimateSize()} reports the characteristics of the spliterator
	 * of the one input stream for a concatenation of that one input stream.
	 */
	@Test
	public void testSpliteratorCharacteristicsOneInput() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 0, false);
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
			stream.spliterator().characteristics(), "ref 1x0");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			intStream.spliterator().characteristics(), "int 1x0");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			longStream.spliterator().characteristics(), "long 1x0");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			doubleStream.spliterator().characteristics(), "double 1x0");
		stream = StreamConcatenationTest.refStream(1, 1, false);
		intStream = StreamConcatenationTest.intStream(1, 1, false);
		longStream = StreamConcatenationTest.longStream(1, 1, false);
		doubleStream = StreamConcatenationTest.doubleStream(1, 1, false);
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
			stream.spliterator().characteristics(), "ref 1x1");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			intStream.spliterator().characteristics(), "int 1x1");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			longStream.spliterator().characteristics(), "long 1x1");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			doubleStream.spliterator().characteristics(), "double 1x1");
		stream = StreamConcatenationTest.refStream(1, 3, false);
		intStream = StreamConcatenationTest.intStream(1, 3, false);
		longStream = StreamConcatenationTest.longStream(1, 3, false);
		doubleStream = StreamConcatenationTest.doubleStream(1, 3, false);
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
			stream.spliterator().characteristics(), "ref 1x3");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			intStream.spliterator().characteristics(), "int 1x3");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			longStream.spliterator().characteristics(), "long 1x3");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			doubleStream.spliterator().characteristics(), "double 1x3");
	}

	/**
	 * Tests that {@link Spliterator#characteristics()} for a concatenation of streams reports the
	 * same characteristics before and after iteration.
	 */
	@Test
	public void testSpliteratorCharacteristicsUnchangedByIteration() {
		Stream<String> stream = StreamConcatenationTest.refStream(3, 1, false);
		IntStream intStream = StreamConcatenationTest.intStream(3, 1, false);
		LongStream longStream = StreamConcatenationTest.longStream(3, 1, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(3, 1, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
			spliterator.characteristics(), "ref before");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			intSpliterator.characteristics(), "int before");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			longSpliterator.characteristics(), "long before");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			doubleSpliterator.characteristics(), "double before");
		spliterator.forEachRemaining(x -> {
		});
		intSpliterator.forEachRemaining((int x) -> {
		});
		longSpliterator.forEachRemaining((long x) -> {
		});
		doubleSpliterator.forEachRemaining((double x) -> {
		});
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED,
			spliterator.characteristics(), "ref after");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			intSpliterator.characteristics(), "int after");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			longSpliterator.characteristics(), "long after");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE,
			doubleSpliterator.characteristics(), "double after");
	}

	/**
	 * Tests that {@link Spliterator#characteristics()} for a concatenation of streams does not
	 * report the {@link Spliterator#SIZED} or {@link Spliterator#SUBSIZED} characteristics when the
	 * input streams each have a known size but the sum of their sizes overflows a {@code long}.
	 */
	@Test
	public void testSpliteratorCharacteristicsSizeOverflow() {
		SplittableRandom random = new SplittableRandom();
		Stream<Integer> s1;
		Stream<Integer> s2;
		Stream<Integer> stream;
		IntStream intS1;
		IntStream intS2;
		IntStream intStream;
		LongStream longS1;
		LongStream longS2;
		LongStream longStream;
		DoubleStream doubleS1;
		DoubleStream doubleS2;
		DoubleStream doubleStream;
		s1 = random.ints(Long.MAX_VALUE / 2).boxed();
		s2 = random.ints(Long.MAX_VALUE / 2).boxed();
		stream = StreamConcatenation.concat(s1, s2);
		intS1 = random.ints(Long.MAX_VALUE / 2);
		intS2 = random.ints(Long.MAX_VALUE / 2);
		intStream = StreamConcatenation.concatInt(intS1, intS2);
		longS1 = random.longs(Long.MAX_VALUE / 2);
		longS2 = random.longs(Long.MAX_VALUE / 2);
		longStream = StreamConcatenation.concatLong(longS1, longS2);
		doubleS1 = random.doubles(Long.MAX_VALUE / 2);
		doubleS2 = random.doubles(Long.MAX_VALUE / 2);
		doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2);
		Assertions.assertEquals(Spliterator.SIZED | Spliterator.SUBSIZED, stream.spliterator().characteristics(),
			"ref no overflow");
		Assertions.assertEquals(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.NONNULL,
			intStream.spliterator().characteristics(), "int no overflow");
		Assertions.assertEquals(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.NONNULL,
			longStream.spliterator().characteristics(), "long no overflow");
		Assertions.assertEquals(Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.NONNULL,
			doubleStream.spliterator().characteristics(), "double no overflow");
		s1 = random.ints(Long.MAX_VALUE).boxed();
		s2 = random.ints(Long.MAX_VALUE).boxed();
		stream = StreamConcatenation.concat(s1, s2);
		intS1 = random.ints(Long.MAX_VALUE);
		intS2 = random.ints(Long.MAX_VALUE);
		intStream = StreamConcatenation.concatInt(intS1, intS2);
		longS1 = random.longs(Long.MAX_VALUE);
		longS2 = random.longs(Long.MAX_VALUE);
		longStream = StreamConcatenation.concatLong(longS1, longS2);
		doubleS1 = random.doubles(Long.MAX_VALUE);
		doubleS2 = random.doubles(Long.MAX_VALUE);
		doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2);
		Assertions.assertEquals(0, stream.spliterator().characteristics(), "ref overflow");
		Assertions.assertEquals(Spliterator.IMMUTABLE | Spliterator.NONNULL, intStream.spliterator().characteristics(),
			"int overflow");
		Assertions.assertEquals(Spliterator.IMMUTABLE | Spliterator.NONNULL, longStream.spliterator().characteristics(),
			"long overflow");
		Assertions.assertEquals(Spliterator.IMMUTABLE | Spliterator.NONNULL,
			doubleStream.spliterator().characteristics(), "double overflow");
	}

	/**
	 * Tests that {@link Spliterator#estimateSize()} for a concatenation of streams returns
	 * {@link Long#MAX_VALUE} when the input streams each have a known size but the sum of their
	 * sizes overflows a {@code long}.
	 */
	@Test
	public void testSpliteratorEstimateSizeOverflow() {
		SplittableRandom random = new SplittableRandom();
		Stream<Integer> s1;
		Stream<Integer> s2;
		Stream<Integer> stream;
		IntStream intS1;
		IntStream intS2;
		IntStream intStream;
		LongStream longS1;
		LongStream longS2;
		LongStream longStream;
		DoubleStream doubleS1;
		DoubleStream doubleS2;
		DoubleStream doubleStream;
		s1 = random.ints(Long.MAX_VALUE / 2).boxed();
		s2 = random.ints(Long.MAX_VALUE / 2).boxed();
		stream = StreamConcatenation.concat(s1, s2);
		intS1 = random.ints(Long.MAX_VALUE / 2);
		intS2 = random.ints(Long.MAX_VALUE / 2);
		intStream = StreamConcatenation.concatInt(intS1, intS2);
		longS1 = random.longs(Long.MAX_VALUE / 2);
		longS2 = random.longs(Long.MAX_VALUE / 2);
		longStream = StreamConcatenation.concatLong(longS1, longS2);
		doubleS1 = random.doubles(Long.MAX_VALUE / 2);
		doubleS2 = random.doubles(Long.MAX_VALUE / 2);
		doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2);
		Assertions.assertEquals(Long.MAX_VALUE - 1, stream.spliterator().estimateSize(), "ref no overflow");
		Assertions.assertEquals(Long.MAX_VALUE - 1, intStream.spliterator().estimateSize(), "int no overflow");
		Assertions.assertEquals(Long.MAX_VALUE - 1, longStream.spliterator().estimateSize(), "long no overflow");
		Assertions.assertEquals(Long.MAX_VALUE - 1, doubleStream.spliterator().estimateSize(), "double no overflow");
		s1 = random.ints(Long.MAX_VALUE).boxed();
		s2 = random.ints(Long.MAX_VALUE).boxed();
		stream = StreamConcatenation.concat(s1, s2);
		intS1 = random.ints(Long.MAX_VALUE);
		intS2 = random.ints(Long.MAX_VALUE);
		intStream = StreamConcatenation.concatInt(intS1, intS2);
		longS1 = random.longs(Long.MAX_VALUE);
		longS2 = random.longs(Long.MAX_VALUE);
		longStream = StreamConcatenation.concatLong(longS1, longS2);
		doubleS1 = random.doubles(Long.MAX_VALUE);
		doubleS2 = random.doubles(Long.MAX_VALUE);
		doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2);
		Assertions.assertEquals(Long.MAX_VALUE, stream.spliterator().estimateSize(), "ref overflow");
		Assertions.assertEquals(Long.MAX_VALUE, intStream.spliterator().estimateSize(), "int overflow");
		Assertions.assertEquals(Long.MAX_VALUE, longStream.spliterator().estimateSize(), "long overflow");
		Assertions.assertEquals(Long.MAX_VALUE, doubleStream.spliterator().estimateSize(), "double overflow");
	}

	/**
	 * Tests that multiple invocations of {@link Spliterator#trySplit()} for a concatenation of
	 * streams may be used to produce spliterators equivalent to the spliterators of the input
	 * streams.
	 */
	@Test
	public void testSpliteratorTrySplitBackToInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(3, 3, false);
		IntStream intStream = StreamConcatenationTest.intStream(3, 3, false);
		LongStream longStream = StreamConcatenationTest.longStream(3, 3, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(3, 3, false);
		Spliterator<String> right = stream.spliterator();
		Spliterator<String> left = right.trySplit();
		Spliterator<String> mid = right.trySplit();
		Spliterator.OfInt intRight = intStream.spliterator();
		Spliterator.OfInt intLeft = intRight.trySplit();
		Spliterator.OfInt intMid = intRight.trySplit();
		Spliterator.OfLong longRight = longStream.spliterator();
		Spliterator.OfLong longLeft = longRight.trySplit();
		Spliterator.OfLong longMid = longRight.trySplit();
		Spliterator.OfDouble doubleRight = doubleStream.spliterator();
		Spliterator.OfDouble doubleLeft = doubleRight.trySplit();
		Spliterator.OfDouble doubleMid = doubleRight.trySplit();
		left.tryAdvance(x -> Assertions.assertEquals("0,0", x, "ref left 1"));
		left.tryAdvance(x -> Assertions.assertEquals("0,1", x, "ref left 2"));
		left.tryAdvance(x -> Assertions.assertEquals("0,2", x, "ref left 3"));
		left.tryAdvance(x -> Assertions.fail("ref left end"));
		mid.tryAdvance(x -> Assertions.assertEquals("1,0", x, "ref mid 1"));
		mid.tryAdvance(x -> Assertions.assertEquals("1,1", x, "ref mid 2"));
		mid.tryAdvance(x -> Assertions.assertEquals("1,2", x, "ref mid 3"));
		mid.tryAdvance(x -> Assertions.fail("ref mid end"));
		right.tryAdvance(x -> Assertions.assertEquals("2,0", x, "ref right 1"));
		right.tryAdvance(x -> Assertions.assertEquals("2,1", x, "ref right 2"));
		right.tryAdvance(x -> Assertions.assertEquals("2,2", x, "ref right 3"));
		right.tryAdvance(x -> Assertions.fail("ref right end"));
		intLeft.tryAdvance((int x) -> Assertions.assertEquals(0, x, "int left 1"));
		intLeft.tryAdvance((int x) -> Assertions.assertEquals(1, x, "int left 2"));
		intLeft.tryAdvance((int x) -> Assertions.assertEquals(2, x, "int left 3"));
		intLeft.tryAdvance((int x) -> Assertions.fail("int left end"));
		intMid.tryAdvance((int x) -> Assertions.assertEquals(3, x, "int mid 1"));
		intMid.tryAdvance((int x) -> Assertions.assertEquals(4, x, "int mid 2"));
		intMid.tryAdvance((int x) -> Assertions.assertEquals(5, x, "int mid 3"));
		intMid.tryAdvance((int x) -> Assertions.fail("int mid end"));
		intRight.tryAdvance((int x) -> Assertions.assertEquals(6, x, "int right 1"));
		intRight.tryAdvance((int x) -> Assertions.assertEquals(7, x, "int right 2"));
		intRight.tryAdvance((int x) -> Assertions.assertEquals(8, x, "int right 3"));
		intRight.tryAdvance((int x) -> Assertions.fail("int right end"));
		longLeft.tryAdvance((long x) -> Assertions.assertEquals(0L, x, "int left 1"));
		longLeft.tryAdvance((long x) -> Assertions.assertEquals(1L, x, "int left 2"));
		longLeft.tryAdvance((long x) -> Assertions.assertEquals(2L, x, "int left 3"));
		longLeft.tryAdvance((long x) -> Assertions.fail("int left end"));
		longMid.tryAdvance((long x) -> Assertions.assertEquals(3L, x, "int mid 1"));
		longMid.tryAdvance((long x) -> Assertions.assertEquals(4L, x, "int mid 2"));
		longMid.tryAdvance((long x) -> Assertions.assertEquals(5L, x, "int mid 3"));
		longMid.tryAdvance((long x) -> Assertions.fail("int mid end"));
		longRight.tryAdvance((long x) -> Assertions.assertEquals(6L, x, "int right 1"));
		longRight.tryAdvance((long x) -> Assertions.assertEquals(7L, x, "int right 2"));
		longRight.tryAdvance((long x) -> Assertions.assertEquals(8L, x, "int right 3"));
		longRight.tryAdvance((long x) -> Assertions.fail("int right end"));
		doubleLeft.tryAdvance((double x) -> Assertions.assertEquals(0d, x, 0.1, "int left 1"));
		doubleLeft.tryAdvance((double x) -> Assertions.assertEquals(1d, x, 0.1, "int left 2"));
		doubleLeft.tryAdvance((double x) -> Assertions.assertEquals(2d, x, 0.1, "int left 3"));
		doubleLeft.tryAdvance((double x) -> Assertions.fail("int left end"));
		doubleMid.tryAdvance((double x) -> Assertions.assertEquals(3d, x, 0.1, "int mid 1"));
		doubleMid.tryAdvance((double x) -> Assertions.assertEquals(4d, x, 0.1, "int mid 2"));
		doubleMid.tryAdvance((double x) -> Assertions.assertEquals(5d, x, 0.1, "int mid 3"));
		doubleMid.tryAdvance((double x) -> Assertions.fail("int mid end"));
		doubleRight.tryAdvance((double x) -> Assertions.assertEquals(6d, x, 0.1, "int right 1"));
		doubleRight.tryAdvance((double x) -> Assertions.assertEquals(7d, x, 0.1, "int right 2"));
		doubleRight.tryAdvance((double x) -> Assertions.assertEquals(8d, x, 0.1, "int right 3"));
		doubleRight.tryAdvance((double x) -> Assertions.fail("int right end"));
	}

	/**
	 * Tests that {@link Spliterator#getComparator()} for a concatenation of zero input streams
	 * throws an {@link IllegalStateException}.
	 */
	@Test
	public void testSpliteratorGetComparatorInvalidForNoInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(0, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(0, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(0, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(0, 0, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		try {
			spliterator.getComparator();
			Assertions.fail("ref");
		} catch (IllegalStateException expected) {
		}
		try {
			intSpliterator.getComparator();
			Assertions.fail("int");
		} catch (IllegalStateException expected) {
		}
		try {
			longSpliterator.getComparator();
			Assertions.fail("long");
		} catch (IllegalStateException expected) {
		}
		try {
			doubleSpliterator.getComparator();
			Assertions.fail("double");
		} catch (IllegalStateException expected) {
		}
	}

	/**
	 * Tests that {@link Spliterator#getComparator()} for a concatenation of one input stream that
	 * is not {@link Spliterator#SORTED} throws an {@link IllegalStateException}.
	 */
	@Test
	public void testSpliteratorGetComparatorInvalidForOneUnsortedInput() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 0, false);
		IntStream intStream = StreamConcatenationTest.intStream(1, 0, false);
		LongStream longStream = StreamConcatenationTest.longStream(1, 0, false);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 0, false);
		Spliterator<String> spliterator = stream.spliterator();
		Spliterator.OfInt intSpliterator = intStream.spliterator();
		Spliterator.OfLong longSpliterator = longStream.spliterator();
		Spliterator.OfDouble doubleSpliterator = doubleStream.spliterator();
		try {
			spliterator.getComparator();
			Assertions.fail("ref 1x0");
		} catch (IllegalStateException expected) {
		}
		try {
			intSpliterator.getComparator();
			Assertions.fail("int 1x0");
		} catch (IllegalStateException expected) {
		}
		try {
			longSpliterator.getComparator();
			Assertions.fail("long 1x0");
		} catch (IllegalStateException expected) {
		}
		try {
			doubleSpliterator.getComparator();
			Assertions.fail("double 1x0");
		} catch (IllegalStateException expected) {
		}
		stream = StreamConcatenationTest.refStream(1, 1, false);
		intStream = StreamConcatenationTest.intStream(1, 1, false);
		longStream = StreamConcatenationTest.longStream(1, 1, false);
		doubleStream = StreamConcatenationTest.doubleStream(1, 1, false);
		spliterator = stream.spliterator();
		intSpliterator = intStream.spliterator();
		longSpliterator = longStream.spliterator();
		doubleSpliterator = doubleStream.spliterator();
		try {
			spliterator.getComparator();
			Assertions.fail("ref 1x1");
		} catch (IllegalStateException expected) {
		}
		try {
			intSpliterator.getComparator();
			Assertions.fail("int 1x1");
		} catch (IllegalStateException expected) {
		}
		try {
			longSpliterator.getComparator();
			Assertions.fail("long 1x1");
		} catch (IllegalStateException expected) {
		}
		try {
			doubleSpliterator.getComparator();
			Assertions.fail("double 1x1");
		} catch (IllegalStateException expected) {
		}
		stream = StreamConcatenationTest.refStream(1, 3, false);
		intStream = StreamConcatenationTest.intStream(1, 3, false);
		longStream = StreamConcatenationTest.longStream(1, 3, false);
		doubleStream = StreamConcatenationTest.doubleStream(1, 3, false);
		spliterator = stream.spliterator();
		intSpliterator = intStream.spliterator();
		longSpliterator = longStream.spliterator();
		doubleSpliterator = doubleStream.spliterator();
		try {
			spliterator.getComparator();
			Assertions.fail("ref 1xmany");
		} catch (IllegalStateException expected) {
		}
		try {
			intSpliterator.getComparator();
			Assertions.fail("int 1xmany");
		} catch (IllegalStateException expected) {
		}
		try {
			longSpliterator.getComparator();
			Assertions.fail("long 1xmany");
		} catch (IllegalStateException expected) {
		}
		try {
			doubleSpliterator.getComparator();
			Assertions.fail("double 1xmany");
		} catch (IllegalStateException expected) {
		}
	}

	/**
	 * Tests that {@link Spliterator#getComparator()} for a concatenation of one input stream that
	 * is {@link Spliterator#SORTED} does not throw an {@link IllegalStateException}.
	 */
	@Test
	public void testSpliteratorGetComparatorValidForOneSortedInput() {
		Stream<String> stream = StreamConcatenationTest.refStream(1, 0, true);
		IntStream intStream = StreamConcatenationTest.intStream(1, 0, true);
		LongStream longStream = StreamConcatenationTest.longStream(1, 0, true);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(1, 0, true);
		stream.spliterator().getComparator();
		intStream.spliterator().getComparator();
		longStream.spliterator().getComparator();
		doubleStream.spliterator().getComparator();
		stream = StreamConcatenationTest.refStream(1, 1, true);
		intStream = StreamConcatenationTest.intStream(1, 1, true);
		longStream = StreamConcatenationTest.longStream(1, 1, true);
		doubleStream = StreamConcatenationTest.doubleStream(1, 1, true);
		stream.spliterator().getComparator();
		intStream.spliterator().getComparator();
		longStream.spliterator().getComparator();
		doubleStream.spliterator().getComparator();
		stream = StreamConcatenationTest.refStream(1, 3, true);
		intStream = StreamConcatenationTest.intStream(1, 3, true);
		longStream = StreamConcatenationTest.longStream(1, 3, true);
		doubleStream = StreamConcatenationTest.doubleStream(1, 3, true);
		stream.spliterator().getComparator();
		intStream.spliterator().getComparator();
		longStream.spliterator().getComparator();
		doubleStream.spliterator().getComparator();
	}

	/**
	 * Tests that {@link Spliterator#getComparator()} for a concatenation of multiple input streams
	 * throws an {@link IllegalStateException}, even when all of those input streams are
	 * {@link Spliterator#SORTED}. However, invocations of {@link Spliterator#trySplit()} should
	 * eventually produce spliterators that report their comparators without throwing.
	 */
	@Test
	public void testSpliteratorGetComparatorInvalidForMultipleInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(3, 1, true);
		IntStream intStream = StreamConcatenationTest.intStream(3, 1, true);
		LongStream longStream = StreamConcatenationTest.longStream(3, 1, true);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(3, 1, true);
		Spliterator<String> right = stream.spliterator();
		Spliterator.OfInt intRight = intStream.spliterator();
		Spliterator.OfLong longRight = longStream.spliterator();
		Spliterator.OfDouble doubleRight = doubleStream.spliterator();
		try {
			right.getComparator();
			Assertions.fail("ref");
		} catch (IllegalStateException expected) {
		}
		try {
			intRight.getComparator();
			Assertions.fail("int");
		} catch (IllegalStateException expected) {
		}
		try {
			longRight.getComparator();
			Assertions.fail("long");
		} catch (IllegalStateException expected) {
		}
		try {
			doubleRight.getComparator();
			Assertions.fail("double");
		} catch (IllegalStateException expected) {
		}
		Spliterator<String> left = right.trySplit();
		Spliterator<String> mid = right.trySplit();
		Spliterator.OfInt intLeft = intRight.trySplit();
		Spliterator.OfInt intMid = intRight.trySplit();
		Spliterator.OfLong longLeft = longRight.trySplit();
		Spliterator.OfLong longMid = longRight.trySplit();
		Spliterator.OfDouble doubleLeft = doubleRight.trySplit();
		Spliterator.OfDouble doubleMid = doubleRight.trySplit();
		left.getComparator();
		mid.getComparator();
		right.getComparator();
		intLeft.getComparator();
		intMid.getComparator();
		intRight.getComparator();
		longLeft.getComparator();
		longMid.getComparator();
		longRight.getComparator();
		doubleLeft.getComparator();
		doubleMid.getComparator();
		doubleRight.getComparator();
	}

	/**
	 * Tests that {@link Spliterator#characteristics()} for a concatenation of multiple input
	 * streams will <em>not</em> report {@link Spliterator#DISTINCT} or {@link Spliterator#SORTED},
	 * even when all of those input spliterators report those characteristics. However, invocations
	 * of {@link Spliterator#trySplit()} should eventually produce spliterators with those
	 * characteristics.
	 */
	@Test
	public void testSpliteratorCharacteristicsNotDistinctOrSortedForMultipleInputs() {
		Stream<String> stream = StreamConcatenationTest.refStream(3, 1, true);
		IntStream intStream = StreamConcatenationTest.intStream(3, 1, true);
		LongStream longStream = StreamConcatenationTest.longStream(3, 1, true);
		DoubleStream doubleStream = StreamConcatenationTest.doubleStream(3, 1, true);
		Spliterator<String> right = stream.spliterator();
		Spliterator.OfInt intRight = intStream.spliterator();
		Spliterator.OfLong longRight = longStream.spliterator();
		Spliterator.OfDouble doubleRight = doubleStream.spliterator();
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED, right.characteristics(), "ref all");
		Assertions.assertEquals(Spliterator.ORDERED, intRight.characteristics(), "int all");
		Assertions.assertEquals(Spliterator.ORDERED, longRight.characteristics(), "long all");
		Assertions.assertEquals(Spliterator.ORDERED, doubleRight.characteristics(), "double all");
		Spliterator<String> left = right.trySplit();
		Spliterator<String> mid = right.trySplit();
		Spliterator.OfInt intLeft = intRight.trySplit();
		Spliterator.OfInt intMid = intRight.trySplit();
		Spliterator.OfLong longLeft = longRight.trySplit();
		Spliterator.OfLong longMid = longRight.trySplit();
		Spliterator.OfDouble doubleLeft = doubleRight.trySplit();
		Spliterator.OfDouble doubleMid = doubleRight.trySplit();
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.SORTED,
			right.characteristics(), "ref right");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.SORTED,
			left.characteristics(), "ref left");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.SORTED,
			mid.characteristics(), "ref mid");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SORTED, intRight.characteristics(), "int right");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SORTED, intLeft.characteristics(), "int left");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SORTED, intMid.characteristics(), "int mid");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SORTED, longRight.characteristics(), "long right");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SORTED, longLeft.characteristics(), "long left");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SORTED, longMid.characteristics(), "long mid");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SORTED, doubleRight.characteristics(),
			"double right");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SORTED, doubleLeft.characteristics(), "double left");
		Assertions.assertEquals(Spliterator.ORDERED | Spliterator.SORTED, doubleMid.characteristics(), "double mid");
	}

	/**
	 * Tests that a concatenation of streams is not parallel when none of the input streams is
	 * parallel.
	 */
	@Test
	public void testIsNotParallelWhenNonParallel() {
		Stream<String> s1 = Stream.of("a");
		Stream<String> s2 = Stream.of("b");
		Stream<String> s3 = Stream.of("c");
		Stream<String> stream = StreamConcatenation.concat(s1, s2, s3);
		IntStream intS1 = IntStream.of(1);
		IntStream intS2 = IntStream.of(2);
		IntStream intS3 = IntStream.of(3);
		IntStream intStream = StreamConcatenation.concatInt(intS1, intS2, intS3);
		LongStream longS1 = LongStream.of(1L);
		LongStream longS2 = LongStream.of(2L);
		LongStream longS3 = LongStream.of(3L);
		LongStream longStream = StreamConcatenation.concatLong(longS1, longS2, longS3);
		DoubleStream doubleS1 = DoubleStream.of(1d);
		DoubleStream doubleS2 = DoubleStream.of(2d);
		DoubleStream doubleS3 = DoubleStream.of(3d);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2, doubleS3);
		Assertions.assertFalse(stream.isParallel(), "ref");
		Assertions.assertFalse(intStream.isParallel(), "int");
		Assertions.assertFalse(longStream.isParallel(), "long");
		Assertions.assertFalse(doubleStream.isParallel(), "double");
	}

	/**
	 * Tests that a concatenation of streams is parallel when one of the input streams is parallel.
	 */
	@Test
	public void testIsParallelWhenOneParallel() {
		Stream<String> s1 = Stream.of("a");
		Stream<String> s2 = Stream.of("b").parallel();
		Stream<String> s3 = Stream.of("c");
		Stream<String> stream = StreamConcatenation.concat(s1, s2, s3);
		IntStream intS1 = IntStream.of(1);
		IntStream intS2 = IntStream.of(2).parallel();
		IntStream intS3 = IntStream.of(3);
		IntStream intStream = StreamConcatenation.concatInt(intS1, intS2, intS3);
		LongStream longS1 = LongStream.of(1L);
		LongStream longS2 = LongStream.of(2L).parallel();
		LongStream longS3 = LongStream.of(3L);
		LongStream longStream = StreamConcatenation.concatLong(longS1, longS2, longS3);
		DoubleStream doubleS1 = DoubleStream.of(1d);
		DoubleStream doubleS2 = DoubleStream.of(2d).parallel();
		DoubleStream doubleS3 = DoubleStream.of(3d);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2, doubleS3);
		Assertions.assertTrue(stream.isParallel(), "ref");
		Assertions.assertTrue(intStream.isParallel(), "int");
		Assertions.assertTrue(longStream.isParallel(), "long");
		Assertions.assertTrue(doubleStream.isParallel(), "double");
	}

	/**
	 * Tests that concatenation is a terminal operation for the input streams.
	 */
	@Test
	public void testIsTerminal() {
		Stream<String> s1 = Stream.of("a");
		Stream<String> s2 = Stream.of("b");
		Stream<String> s3 = Stream.of("c");
		StreamConcatenation.concat(s1, s2, s3);
		IntStream intS1 = IntStream.of(1);
		IntStream intS2 = IntStream.of(2);
		IntStream intS3 = IntStream.of(3);
		StreamConcatenation.concatInt(intS1, intS2, intS3);
		LongStream longS1 = LongStream.of(1L);
		LongStream longS2 = LongStream.of(2L);
		LongStream longS3 = LongStream.of(3L);
		StreamConcatenation.concatLong(longS1, longS2, longS3);
		DoubleStream doubleS1 = DoubleStream.of(1d);
		DoubleStream doubleS2 = DoubleStream.of(2d);
		DoubleStream doubleS3 = DoubleStream.of(3d);
		StreamConcatenation.concatDouble(doubleS1, doubleS2, doubleS3);
		try {
			s1.forEach(x -> {
			});
			Assertions.fail("ref 1");
		} catch (IllegalStateException expected) {
		}
		try {
			s2.forEach(x -> {
			});
			Assertions.fail("ref 2");
		} catch (IllegalStateException expected) {
		}
		try {
			s3.forEach(x -> {
			});
			Assertions.fail("ref 3");
		} catch (IllegalStateException expected) {
		}
		try {
			intS1.forEach(x -> {
			});
			Assertions.fail("int 1");
		} catch (IllegalStateException expected) {
		}
		try {
			intS2.forEach(x -> {
			});
			Assertions.fail("int 2");
		} catch (IllegalStateException expected) {
		}
		try {
			intS3.forEach(x -> {
			});
			Assertions.fail("int 3");
		} catch (IllegalStateException expected) {
		}
		try {
			longS1.forEach(x -> {
			});
			Assertions.fail("long 1");
		} catch (IllegalStateException expected) {
		}
		try {
			longS2.forEach(x -> {
			});
			Assertions.fail("long 2");
		} catch (IllegalStateException expected) {
		}
		try {
			longS3.forEach(x -> {
			});
			Assertions.fail("long 3");
		} catch (IllegalStateException expected) {
		}
		try {
			doubleS1.forEach(x -> {
			});
			Assertions.fail("double 1");
		} catch (IllegalStateException expected) {
		}
		try {
			doubleS2.forEach(x -> {
			});
			Assertions.fail("double 2");
		} catch (IllegalStateException expected) {
		}
		try {
			doubleS3.forEach(x -> {
			});
			Assertions.fail("double 3");
		} catch (IllegalStateException expected) {
		}
	}

	/**
	 * Tests that a concatenation of zero input streams contains the expected elements (none).
	 */
	@Test
	public void testContainsExpectedElementsWhenNoStreams() {
		@SuppressWarnings("unchecked")
		Stream<String> stream = StreamConcatenation.concat((Stream<String>[]) new Stream<?>[0]);
		IntStream intStream = StreamConcatenation.concatInt(new IntStream[0]);
		LongStream longStream = StreamConcatenation.concatLong(new LongStream[0]);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(new DoubleStream[0]);
		List<String> out = stream.collect(Collectors.toList());
		List<Integer> intOut = intStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		List<Long> longOut = longStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		List<Double> doubleOut = doubleStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		Assertions.assertEquals(Collections.<String> emptyList(), out, "ref");
		Assertions.assertEquals(Collections.<Integer> emptyList(), intOut, "int");
		Assertions.assertEquals(Collections.<Long> emptyList(), longOut, "long");
		Assertions.assertEquals(Collections.<Double> emptyList(), doubleOut, "double");
	}

	/**
	 * Tests that a concatenation of a single input stream contains the expected elements (the
	 * elements of that input stream).
	 */
	@Test
	public void testContainsExpectedElementsWhenOneStream() {
		Stream<String> s1 = Stream.of("a", "b", "c");
		IntStream intS1 = IntStream.of(1, 2, 3);
		LongStream longS1 = LongStream.of(1L, 2L, 3L);
		DoubleStream doubleS1 = DoubleStream.of(1d, 2d, 3d);
		Stream<String> stream = StreamConcatenation.concat(s1);
		IntStream intStream = StreamConcatenation.concatInt(intS1);
		LongStream longStream = StreamConcatenation.concatLong(longS1);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(doubleS1);
		List<String> out = stream.collect(Collectors.toList());
		List<Integer> intOut = intStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		List<Long> longOut = longStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		List<Double> doubleOut = doubleStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		Assertions.assertEquals(Arrays.asList("a", "b", "c"), out, "ref");
		Assertions.assertEquals(Arrays.asList(1, 2, 3), intOut, "int");
		Assertions.assertEquals(Arrays.asList(1L, 2L, 3L), longOut, "long");
		Assertions.assertEquals(Arrays.asList(1d, 2d, 3d), doubleOut, "double");
	}

	/**
	 * Tests that a concatenation of multiple input streams contains the expected elements (the
	 * elements of each of the input streams).
	 */
	@Test
	public void testContainsExpectedElementsWhenMultipleStreams() {
		Stream<String> s1 = Stream.of("a", "b", "c");
		Stream<String> s2 = Stream.of("d", "e");
		Stream<String> s3 = Stream.of("f");
		Stream<String> stream = StreamConcatenation.concat(s1, s2, s3);
		IntStream intS1 = IntStream.of(1, 2, 3);
		IntStream intS2 = IntStream.of(4, 5);
		IntStream intS3 = IntStream.of(6);
		IntStream intStream = StreamConcatenation.concatInt(intS1, intS2, intS3);
		LongStream longS1 = LongStream.of(1L, 2L, 3L);
		LongStream longS2 = LongStream.of(4L, 5L);
		LongStream longS3 = LongStream.of(6L);
		LongStream longStream = StreamConcatenation.concatLong(longS1, longS2, longS3);
		DoubleStream doubleS1 = DoubleStream.of(1d, 2d, 3d);
		DoubleStream doubleS2 = DoubleStream.of(4d, 5d);
		DoubleStream doubleS3 = DoubleStream.of(6d);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2, doubleS3);
		List<String> out = stream.collect(Collectors.toList());
		List<Integer> intOut = intStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		List<Long> longOut = longStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		List<Double> doubleOut = doubleStream.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), out, "ref");
		Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), intOut, "int");
		Assertions.assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), longOut, "long");
		Assertions.assertEquals(Arrays.asList(1d, 2d, 3d, 4d, 5d, 6d), doubleOut, "double");
	}

	/**
	 * Tests that closing a concatenation of zero input streams completes without throwing an
	 * exception.
	 */
	@Test
	public void testCloseNone() {
		@SuppressWarnings("unchecked")
		Stream<String> stream = StreamConcatenation.concat((Stream<String>[]) new Stream<?>[0]);
		IntStream intStream = StreamConcatenation.concatInt(new IntStream[0]);
		LongStream longStream = StreamConcatenation.concatLong(new LongStream[0]);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(new DoubleStream[0]);
		stream.close();
		intStream.close();
		longStream.close();
		doubleStream.close();
	}

	/**
	 * Tests that closing a concatenation of a single input stream closes that input stream.
	 */
	@Test
	public void testCloseOne() {
		LongAdder counter = new LongAdder();
		LongAdder intCounter = new LongAdder();
		LongAdder longCounter = new LongAdder();
		LongAdder doubleCounter = new LongAdder();
		Stream<String> s1 = Stream.of("a").onClose(counter::increment);
		IntStream intS1 = IntStream.of(1).onClose(intCounter::increment);
		LongStream longS1 = LongStream.of(1L).onClose(longCounter::increment);
		DoubleStream doubleS1 = DoubleStream.of(1d).onClose(doubleCounter::increment);
		Stream<String> stream = StreamConcatenation.concat(s1);
		IntStream intStream = StreamConcatenation.concatInt(intS1);
		LongStream longStream = StreamConcatenation.concatLong(longS1);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(doubleS1);
		stream.close();
		intStream.close();
		longStream.close();
		doubleStream.close();
		Assertions.assertEquals(1, counter.sum(), "ref");
		Assertions.assertEquals(1, intCounter.sum(), "int");
		Assertions.assertEquals(1, longCounter.sum(), "long");
		Assertions.assertEquals(1, doubleCounter.sum(), "double");
	}

	/**
	 * Tests that closing a concatenation of multiple input streams closes each of the input
	 * streams.
	 */
	@Test
	public void testCloseMultiple() {
		LongAdder c1 = new LongAdder();
		LongAdder c2 = new LongAdder();
		LongAdder c3 = new LongAdder();
		LongAdder intC1 = new LongAdder();
		LongAdder intC2 = new LongAdder();
		LongAdder intC3 = new LongAdder();
		LongAdder longC1 = new LongAdder();
		LongAdder longC2 = new LongAdder();
		LongAdder longC3 = new LongAdder();
		LongAdder doubleC1 = new LongAdder();
		LongAdder doubleC2 = new LongAdder();
		LongAdder doubleC3 = new LongAdder();
		Stream<String> s1 = Stream.of("a").onClose(c1::increment);
		Stream<String> s2 = Stream.of("b").onClose(c2::increment);
		Stream<String> s3 = Stream.of("c").onClose(c3::increment);
		IntStream intS1 = IntStream.of(1).onClose(intC1::increment);
		IntStream intS2 = IntStream.of(2).onClose(intC2::increment);
		IntStream intS3 = IntStream.of(3).onClose(intC3::increment);
		LongStream longS1 = LongStream.of(1L).onClose(longC1::increment);
		LongStream longS2 = LongStream.of(2L).onClose(longC2::increment);
		LongStream longS3 = LongStream.of(3L).onClose(longC3::increment);
		DoubleStream doubleS1 = DoubleStream.of(1d).onClose(doubleC1::increment);
		DoubleStream doubleS2 = DoubleStream.of(2d).onClose(doubleC2::increment);
		DoubleStream doubleS3 = DoubleStream.of(3d).onClose(doubleC3::increment);
		Stream<String> stream = StreamConcatenation.concat(s1, s2, s3);
		IntStream intStream = StreamConcatenation.concatInt(intS1, intS2, intS3);
		LongStream longStream = StreamConcatenation.concatLong(longS1, longS2, longS3);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2, doubleS3);
		stream.close();
		intStream.close();
		longStream.close();
		doubleStream.close();
		Assertions.assertEquals(1, c1.sum(), "ref counter 1");
		Assertions.assertEquals(1, c2.sum(), "ref counter 2");
		Assertions.assertEquals(1, c3.sum(), "ref counter 3");
		Assertions.assertEquals(1, intC1.sum(), "int counter 1");
		Assertions.assertEquals(1, intC2.sum(), "int counter 2");
		Assertions.assertEquals(1, intC3.sum(), "int counter 3");
		Assertions.assertEquals(1, longC1.sum(), "long counter 1");
		Assertions.assertEquals(1, longC2.sum(), "long counter 2");
		Assertions.assertEquals(1, longC3.sum(), "long counter 3");
		Assertions.assertEquals(1, doubleC1.sum(), "double counter 1");
		Assertions.assertEquals(1, doubleC2.sum(), "double counter 2");
		Assertions.assertEquals(1, doubleC3.sum(), "double counter 3");
	}

	static final class E1 extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	static final class E2 extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	static final class E3 extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Tests that closing a concatenation of streams closes each of the input streams even when the
	 * close handlers throw exceptions, and that the first exception thrown is rethrown, and
	 * subsequent exceptions are suppressed in the first.
	 */
	@Test
	public void testCloseWithExceptions() {
		LongAdder c1 = new LongAdder();
		LongAdder c2 = new LongAdder();
		LongAdder c3 = new LongAdder();
		LongAdder intC1 = new LongAdder();
		LongAdder intC2 = new LongAdder();
		LongAdder intC3 = new LongAdder();
		LongAdder longC1 = new LongAdder();
		LongAdder longC2 = new LongAdder();
		LongAdder longC3 = new LongAdder();
		LongAdder doubleC1 = new LongAdder();
		LongAdder doubleC2 = new LongAdder();
		LongAdder doubleC3 = new LongAdder();
		Stream<String> s1 = Stream.of("a").onClose(() -> {
			c1.increment();
			throw new StreamConcatenationTest.E1();
		});
		Stream<String> s2 = Stream.of("b").onClose(() -> {
			c2.increment();
			throw new StreamConcatenationTest.E2();
		});
		Stream<String> s3 = Stream.of("c").onClose(() -> {
			c3.increment();
			throw new StreamConcatenationTest.E3();
		});
		IntStream intS1 = IntStream.of(1).onClose(() -> {
			intC1.increment();
			throw new StreamConcatenationTest.E1();
		});
		IntStream intS2 = IntStream.of(2).onClose(() -> {
			intC2.increment();
			throw new StreamConcatenationTest.E2();
		});
		IntStream intS3 = IntStream.of(3).onClose(() -> {
			intC3.increment();
			throw new StreamConcatenationTest.E3();
		});
		LongStream longS1 = LongStream.of(1L).onClose(() -> {
			longC1.increment();
			throw new StreamConcatenationTest.E1();
		});
		LongStream longS2 = LongStream.of(2L).onClose(() -> {
			longC2.increment();
			throw new StreamConcatenationTest.E2();
		});
		LongStream longS3 = LongStream.of(3L).onClose(() -> {
			longC3.increment();
			throw new StreamConcatenationTest.E3();
		});
		DoubleStream doubleS1 = DoubleStream.of(1d).onClose(() -> {
			doubleC1.increment();
			throw new StreamConcatenationTest.E1();
		});
		DoubleStream doubleS2 = DoubleStream.of(2d).onClose(() -> {
			doubleC2.increment();
			throw new StreamConcatenationTest.E2();
		});
		DoubleStream doubleS3 = DoubleStream.of(3d).onClose(() -> {
			doubleC3.increment();
			throw new StreamConcatenationTest.E3();
		});
		Stream<String> stream = StreamConcatenation.concat(s1, s2, s3);
		IntStream intStream = StreamConcatenation.concatInt(intS1, intS2, intS3);
		LongStream longStream = StreamConcatenation.concatLong(longS1, longS2, longS3);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2, doubleS3);
		try {
			stream.close();
			Assertions.fail("ref close did not throw");
		} catch (Throwable expected) {
			Assertions.assertTrue(expected instanceof StreamConcatenationTest.E1, "ref thrown type");
			Throwable[] suppressed = expected.getSuppressed();
			Assertions.assertEquals(2, suppressed.length, "ref suppressed length");
			Assertions.assertTrue(suppressed[0] instanceof StreamConcatenationTest.E2, "ref suppressed type 1");
			Assertions.assertTrue(suppressed[1] instanceof StreamConcatenationTest.E3, "ref suppressed type 2");
		}
		try {
			intStream.close();
			Assertions.fail("int close did not throw");
		} catch (Throwable expected) {
			Assertions.assertTrue(expected instanceof StreamConcatenationTest.E1, "int thrown type");
			Throwable[] suppressed = expected.getSuppressed();
			Assertions.assertEquals(2, suppressed.length, "int suppressed length");
			Assertions.assertTrue(suppressed[0] instanceof StreamConcatenationTest.E2, "int suppressed type 1");
			Assertions.assertTrue(suppressed[1] instanceof StreamConcatenationTest.E3, "int suppressed type 2");
		}
		try {
			longStream.close();
			Assertions.fail("long close did not throw");
		} catch (Throwable expected) {
			Assertions.assertTrue(expected instanceof StreamConcatenationTest.E1, "long thrown type");
			Throwable[] suppressed = expected.getSuppressed();
			Assertions.assertEquals(2, suppressed.length, "long suppressed length");
			Assertions.assertTrue(suppressed[0] instanceof StreamConcatenationTest.E2, "long suppressed type 1");
			Assertions.assertTrue(suppressed[1] instanceof StreamConcatenationTest.E3, "long suppressed type 2");
		}
		try {
			doubleStream.close();
			Assertions.fail("double close did not throw");
		} catch (Throwable expected) {
			Assertions.assertTrue(expected instanceof StreamConcatenationTest.E1, "double thrown type");
			Throwable[] suppressed = expected.getSuppressed();
			Assertions.assertEquals(2, suppressed.length, "double suppressed length");
			Assertions.assertTrue(suppressed[0] instanceof StreamConcatenationTest.E2, "double suppressed type 1");
			Assertions.assertTrue(suppressed[1] instanceof StreamConcatenationTest.E3, "double suppressed type 2");
		}
		Assertions.assertEquals(1, c1.sum(), "ref counter 1");
		Assertions.assertEquals(1, c2.sum(), "ref counter 2");
		Assertions.assertEquals(1, c3.sum(), "ref counter 3");
		Assertions.assertEquals(1, intC1.sum(), "int counter 1");
		Assertions.assertEquals(1, intC2.sum(), "int counter 2");
		Assertions.assertEquals(1, intC3.sum(), "int counter 3");
		Assertions.assertEquals(1, longC1.sum(), "long counter 1");
		Assertions.assertEquals(1, longC2.sum(), "long counter 2");
		Assertions.assertEquals(1, longC3.sum(), "long counter 3");
		Assertions.assertEquals(1, doubleC1.sum(), "double counter 1");
		Assertions.assertEquals(1, doubleC2.sum(), "double counter 2");
		Assertions.assertEquals(1, doubleC3.sum(), "double counter 3");
	}

	/**
	 * Tests that a concatenation of streams that are infinite results in a stream whose
	 * {@link Stream#findAny()} operation terminates successfully.
	 *
	 * <p>
	 * This test is meant to highlight an advantage over {@code Stream.of(inputs).flatMap(x -> x)},
	 * which would enter an infinite loop upon the call to {@link Stream#findAny()}}.
	 */
	@Test
	public void testFindAnyTerminatesWhenInfiniteStreams() {
		Stream<String> s1 = Stream.generate(() -> "a");
		Stream<String> s2 = Stream.generate(() -> "b");
		Stream<String> s3 = Stream.generate(() -> "c");
		Stream<String> stream = StreamConcatenation.concat(s1, s2, s3);
		IntStream intS1 = IntStream.generate(() -> 1);
		IntStream intS2 = IntStream.generate(() -> 2);
		IntStream intS3 = IntStream.generate(() -> 3);
		IntStream intStream = StreamConcatenation.concatInt(intS1, intS2, intS3);
		LongStream longS1 = LongStream.generate(() -> 1L);
		LongStream longS2 = LongStream.generate(() -> 2L);
		LongStream longS3 = LongStream.generate(() -> 3L);
		LongStream longStream = StreamConcatenation.concatLong(longS1, longS2, longS3);
		DoubleStream doubleS1 = DoubleStream.generate(() -> 1d);
		DoubleStream doubleS2 = DoubleStream.generate(() -> 2d);
		DoubleStream doubleS3 = DoubleStream.generate(() -> 3d);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(doubleS1, doubleS2, doubleS3);
		Assertions.assertTrue(stream.findAny().isPresent(), "ref");
		Assertions.assertTrue(intStream.findAny().isPresent(), "int");
		Assertions.assertTrue(longStream.findAny().isPresent(), "long");
		Assertions.assertTrue(doubleStream.findAny().isPresent(), "double");
	}

	/**
	 * Tests that a concatenation of an extremely large number of input streams results in a stream
	 * whose terminal operations do not incur a {@link StackOverflowError}.
	 *
	 * <p>
	 * This test is meant to highlight an advantage of over
	 * {@code Stream.of(inputs).reduce(Stream::concat).orElseGet(Stream::empty)}, which would throw
	 * a {@link StackOverflowError} if given enough inputs.
	 */
	@Test
	public void testNoStackOverflowWhenSoManyStreams() {
		int numberOfStreams = 100_000;
		String[] elements = {
			"one", "two"
		};
		int[] intElements = {
			1, 2
		};
		long[] longElements = {
			1L, 2L
		};
		double[] doubleElements = {
			1d, 2d
		};
		@SuppressWarnings("unchecked")
		Stream<String>[] inputs = (Stream<String>[]) new Stream<?>[numberOfStreams];
		IntStream[] intInputs = new IntStream[numberOfStreams];
		LongStream[] longInputs = new LongStream[numberOfStreams];
		DoubleStream[] doubleInputs = new DoubleStream[numberOfStreams];
		Arrays.setAll(inputs, i -> Stream.of(elements));
		Arrays.setAll(intInputs, i -> IntStream.of(intElements));
		Arrays.setAll(longInputs, i -> LongStream.of(longElements));
		Arrays.setAll(doubleInputs, i -> DoubleStream.of(doubleElements));
		Stream<String> stream = StreamConcatenation.concat(inputs);
		IntStream intStream = StreamConcatenation.concatInt(intInputs);
		LongStream longStream = StreamConcatenation.concatLong(longInputs);
		DoubleStream doubleStream = StreamConcatenation.concatDouble(doubleInputs);
		Assertions.assertEquals(numberOfStreams * elements.length, stream.count(), "ref");
		Assertions.assertEquals(numberOfStreams * intElements.length, intStream.count(), "int");
		Assertions.assertEquals(numberOfStreams * longElements.length, longStream.count(), "long");
		Assertions.assertEquals(numberOfStreams * doubleElements.length, doubleStream.count(), "double");
	}
}