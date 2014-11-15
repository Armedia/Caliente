package com.delta.cmsmf.utils;

public final class SynchronizedCounter {

	private long value;

	/**
	 * <p>
	 * Create a new counter with the initial value of 0
	 * </p>
	 */
	public SynchronizedCounter() {
		this(0);
	}

	/**
	 * <p>
	 * Create a new counter with the given initial value
	 * </p>
	 *
	 * @param initialValue
	 */
	public SynchronizedCounter(long initialValue) {
		this.value = initialValue;
	}

	/**
	 * <p>
	 * Decrement the counter by 1.
	 * </p>
	 *
	 * @return the new value
	 */
	public synchronized long decrement() {
		return increment(-1);
	}

	/**
	 * <p>
	 * Decrement the counter by the given amount. Signum is preserved, so decrementing by -1 is the
	 * same as incrementing by 1.
	 * </p>
	 *
	 * @return the new value
	 */
	public synchronized long decrement(long amount) {
		return increment(-amount);
	}

	/**
	 * <p>
	 * Increment the counter by 1.
	 * </p>
	 *
	 * @return the new value
	 */
	public synchronized long increment() {
		return increment(1);
	}

	/**
	 * <p>
	 * Increment the counter by the given amount. Signum is preserved, so incrementing by -1 is the
	 * same as decrementing by 1.
	 * </p>
	 *
	 * @return the new value
	 */
	public synchronized long increment(long amount) {
		if (amount != 0) {
			this.value += amount;
			notify();
		}
		return this.value;
	}

	/**
	 * <p>
	 * Sets the value of the internal counter, and returns the previous value.
	 * </p>
	 *
	 * @param value
	 * @return the previous value.
	 */
	public synchronized long setValue(long value) {
		long prev = this.value;
		if (prev != value) {
			// Only notify when there's an actual change
			this.value = value;
			notify();
		}
		return prev;
	}

	public synchronized long getValue() {
		return this.value;
	}

	/**
	 * <p>
	 * Blocks (using {@link Object#wait()}) until the internal counter matches the given value.
	 * </p>
	 *
	 * @param value
	 * @throws InterruptedException
	 */
	public synchronized void waitForValue(long value) throws InterruptedException {
		while (this.value != value) {
			wait();
		}
		notify();
	}

	/**
	 * <p>
	 * Blocks (using {@link Object#wait()}) until the given number of changes are counted. If
	 * {@code count} is less than or equal to 0, it will return immediately without blocking, with
	 * the current value of the counter.
	 * </p>
	 *
	 * @param count
	 * @return the value of the counter at the moment the condition is met
	 * @throws InterruptedException
	 */
	public synchronized long waitUntilChanges(int count) throws InterruptedException {
		if (count <= 0) { return this.value; }
		long ret = 0;
		for (int i = 0; i < count; i++) {
			ret = waitUntilChange();
		}
		return ret;
	}

	/**
	 * <p>
	 * Blocks (using {@link Object#wait()}) until the internal counter's value has changed by at
	 * least 1. This is identical to invoking {@link #waitUntilDelta(int)} with a value of 1.
	 * </p>
	 *
	 * @return the value of the counter at the moment the condition is met
	 * @throws InterruptedException
	 */
	public synchronized long waitUntilChange() throws InterruptedException {
		return waitUntilDelta(1);
	}

	/**
	 * <p>
	 * Blocks (using {@link Object#wait()}) until the internal counter's value has changed by at
	 * least the given {@code delta}. Please note that {@link Math#abs(int)} is used to obtain the
	 * {@code delta}'s absolute value and use that as the magnitude of the expected shift. Thus,
	 * negative or positive values of the same magnitude may be used interchangeably.
	 * </p>
	 * <p>
	 * If the delta is 0, this method returns immediately with the current value of the counter.
	 * </p>
	 *
	 * @param delta
	 * @return the value of the counter at the moment the condition is met
	 * @throws InterruptedException
	 */
	public synchronized long waitUntilDelta(int delta) throws InterruptedException {
		// The delta is an int instead of a long to avoid problems with trying to detect overflow
		// conditions
		delta = Math.abs(delta);
		final long current = this.value;
		while (((current - delta) < this.value) && (this.value < (current + delta))) {
			wait();
		}
		notify();
		return this.value;
	}

	/**
	 * <p>
	 * Blocks (using {@link Object#wait()}) until the counter's value increases by at least one.
	 * Identical to invoking {@link #waitUntilIncrease(int)} with a value of 1.
	 * </p>
	 *
	 * @return the value of the counter at the moment the condition is met
	 * @throws InterruptedException
	 */
	public synchronized long waitUntilIncrease() throws InterruptedException {
		return waitUntilIncrease(1);
	}

	/**
	 * <p>
	 * Blocks (using {@link Object#wait()}) until the counter's value increases by at least the
	 * given delta. If a negative value is provided, then its magnitude is obtained via
	 * {@link Math#abs(int)}.
	 * </p>
	 *
	 * @param delta
	 * @return the value of the counter at the moment the condition is met
	 * @throws InterruptedException
	 */
	public synchronized long waitUntilIncrease(int delta) throws InterruptedException {
		delta = Math.abs(delta);
		final long current = this.value;
		while (this.value < (current + delta)) {
			wait();
		}
		notify();
		return this.value;
	}

	/**
	 * <p>
	 * Blocks (using {@link Object#wait()}) until the counter's value decreases by at least one.
	 * Identical to invoking {@link #waitUntilDecrease(int)} with a value of 1.
	 * </p>
	 *
	 * @return the value of the counter at the moment the condition is met
	 * @throws InterruptedException
	 */
	public synchronized long waitUntilDecrease() throws InterruptedException {
		return waitUntilDecrease(1);
	}

	/**
	 * <p>
	 * <p>
	 * Blocks (using {@link Object#wait()}) until the counter's value decreases by at least the
	 * given delta. If a negative value is provided, then its magnitude is obtained via
	 * {@link Math#abs(int)}.
	 * </p>
	 * </p>
	 *
	 * @param delta
	 * @return the value of the counter at the moment the condition is met
	 * @throws InterruptedException
	 */
	public synchronized long waitUntilDecrease(int delta) throws InterruptedException {
		delta = Math.abs(delta);
		final long current = this.value;
		while (this.value > (current - delta)) {
			wait();
		}
		notify();
		return this.value;
	}
}