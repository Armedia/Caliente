package com.delta.cmsmf.utils;

public final class SynchronizedCounter {

	private long value;

	public SynchronizedCounter() {
		this(0);
	}

	public SynchronizedCounter(long initialValue) {
		this.value = initialValue;
	}

	public synchronized long decrement() {
		return increment(-1);
	}

	public synchronized long decrement(long amount) {
		return increment(-amount);
	}

	public synchronized long increment() {
		return increment(1);
	}

	public synchronized long increment(long amount) {
		this.value += amount;
		notify();
		return this.value;
	}

	public synchronized long setValue(long value) {
		long prev = this.value;
		this.value = value;
		notify();
		return prev;
	}

	public synchronized long getValue() {
		return this.value;
	}

	public synchronized void waitUntilChange() throws InterruptedException {
		final long current = this.value;
		while (this.value == current) {
			wait();
		}
		notify();
	}

	public synchronized void waitForValue(long value) throws InterruptedException {
		while (this.value != value) {
			wait();
		}
		notify();
	}
}