package com.armedia.caliente.engine.tools;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Assert;
import org.junit.Test;

class ReadWriteLockedTest {

	@Test
	void testReadLock() {
		ReadWriteLockable rwl = null;
		ReadWriteLock lock = new ReentrantReadWriteLock();
		Lock readLock = null;

		rwl = new BaseReadWriteLockable(lock);
		Assert.assertSame(lock.readLock(), rwl.readLock());

		readLock = rwl.readLock();

		for (int i = 1; i <= 10; i++) {
			Assert.assertTrue(String.format("Failed to acquire the reading lock on attempt # %d", i),
				readLock.tryLock());
		}
		Assert.assertFalse("Succeeded in acquiring the write lock while the read lock was held",
			rwl.writeLock().tryLock());
		for (int i = 10; i > 0; i--) {
			try {
				readLock.unlock();
			} catch (Exception e) {
				Assert.fail(String.format("Failed to release the reading lock on attempt # %d", i));
			}
		}
		Assert.assertTrue("Failed to acquire the write lock while the read lock was held", rwl.writeLock().tryLock());
		rwl.writeLock().unlock();
	}

	@Test
	void testWriteLock() {
	}

	@Test
	void testReadLockedSupplierOfE() {
	}

	@Test
	void testReadLockedRunnable() {
	}

	@Test
	void testWriteLockedSupplierOfE() {
	}

	@Test
	void testWriteLockedRunnable() {
	}

	@Test
	void testReadUpgradableSupplierOfEPredicateOfEFunctionOfEE() {
	}

	@Test
	void testReadUpgradableSupplierOfEPredicateOfEConsumerOfE() {
	}
}