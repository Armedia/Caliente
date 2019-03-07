package com.armedia.caliente.engine.tools;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Assert;
import org.junit.Test;

public class ReadWriteLockedTest {

	@Test
	public void testGetReadLock() {
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
		final ReadWriteLockable rwl = new BaseReadWriteLockable(lock);

		Assert.assertSame(readLock, rwl.getReadLock());
	}

	@Test
	public void testGetWriteLock() {
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
		final ReadWriteLockable rwl = new BaseReadWriteLockable(lock);

		Assert.assertSame(writeLock, rwl.getWriteLock());
	}

	@Test
	public void testAcquireReadLock() {
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
		final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
		final ReadWriteLockable rwl = new BaseReadWriteLockable(lock);

		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertSame(lock.readLock(), rwl.acquireReadLock());
		Assert.assertEquals(1, lock.getReadHoldCount());
		Assert.assertFalse("Succeeded in acquiring the write lock while the read lock was held",
			rwl.getWriteLock().tryLock());
		Assert.assertEquals(1, lock.getReadHoldCount());
		readLock.unlock();
		Assert.assertEquals(0, lock.getReadHoldCount());

		Assert.assertSame(lock.readLock(), rwl.getReadLock());
		Assert.assertEquals(0, lock.getReadHoldCount());
		try {
			readLock.unlock();
			Assert.fail("The read lock was not held but was unlocked");
		} catch (Exception e) {
			// All is well
		}

		Assert.assertEquals(0, lock.getReadHoldCount());
		for (int i = 1; i <= 10; i++) {
			Assert.assertNotNull(String.format("Failed to acquire the reading lock on attempt # %d", i),
				rwl.acquireReadLock());
		}
		Assert.assertEquals(10, lock.getReadHoldCount());
		Assert.assertFalse("Succeeded in acquiring the write lock while the read lock was held",
			rwl.getWriteLock().tryLock());
		for (int i = 10; i > 0; i--) {
			try {
				readLock.unlock();
			} catch (Exception e) {
				Assert.fail(String.format("Failed to release the reading lock on attempt # %d", i));
			}
		}
		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertTrue("Failed to acquire the write lock while the read lock was not held",
			rwl.getWriteLock().tryLock());
		rwl.getWriteLock().unlock();
	}

	@Test
	public void testAcquireWriteLock() throws Exception {
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
		final ReadWriteLockable rwl = new BaseReadWriteLockable(lock);

		Assert.assertSame(lock.writeLock(), rwl.getWriteLock());

		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertSame(lock.writeLock(), rwl.acquireWriteLock());
		Assert.assertTrue(writeLock.isHeldByCurrentThread());
		writeLock.unlock();
		Assert.assertFalse(writeLock.isHeldByCurrentThread());

		try {
			writeLock.unlock();
			Assert.fail("The write lock was not held but was unlocked");
		} catch (Exception e) {
			// All is well
		}

		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		for (int i = 1; i <= 10; i++) {
			Assert.assertNotNull(String.format("Failed to acquire the writing lock on attempt # %d", i),
				rwl.acquireWriteLock());
			Assert.assertTrue(writeLock.isHeldByCurrentThread());
		}
		Assert.assertTrue("Failed to acquire the read lock while the write lock was held", rwl.getReadLock().tryLock());
		rwl.getReadLock().unlock();

		for (int i = 10; i > 0; i--) {
			Assert.assertTrue(writeLock.isHeldByCurrentThread());
			try {
				writeLock.unlock();
			} catch (Exception e) {
				Assert.fail(String.format("Failed to release the writing lock on attempt # %d", i));
			}
		}
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
	}

	@Test
	public void testReadLocked() {
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
		final ReadWriteLockable rwl = new BaseReadWriteLockable(lock);

		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		ReadWriteLock ret = rwl.readLocked(() -> {
			// Prove that we're holding the read lock
			Assert.assertFalse(writeLock.isHeldByCurrentThread());
			Assert.assertFalse("Acquired the write lock while the read lock was held", writeLock.tryLock());
			return lock;
		});
		Assert.assertSame(lock, ret);
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertTrue(writeLock.tryLock());
		writeLock.unlock();

		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		rwl.readLocked(() -> {
			// Prove that we're holding the read lock
			Assert.assertFalse(writeLock.isHeldByCurrentThread());
			Assert.assertFalse("Acquired the write lock while the read lock was held", writeLock.tryLock());
		});
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertTrue(writeLock.tryLock());
		writeLock.unlock();
	}

	@Test
	public void testWriteLocked() {
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
		final ReadWriteLockable rwl = new BaseReadWriteLockable(lock);

		Assert.assertTrue(writeLock.tryLock());
		Assert.assertTrue(writeLock.isHeldByCurrentThread());
		writeLock.unlock();

		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		ReadWriteLock ret = rwl.writeLocked(() -> {
			// Prove that we're holding the read lock
			Assert.assertTrue(writeLock.isHeldByCurrentThread());
			return lock;
		});
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertSame(lock, ret);

		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		rwl.writeLocked(() -> {
			Assert.assertTrue(writeLock.isHeldByCurrentThread());
		});
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
	}

	@Test
	public void testReadUpgradable() {
		final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
		final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
		final ReadWriteLockable rwl = new BaseReadWriteLockable(lock);

		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertTrue(writeLock.tryLock());
		Assert.assertTrue(writeLock.isHeldByCurrentThread());
		writeLock.unlock();
		Assert.assertFalse(writeLock.isHeldByCurrentThread());

		Assert.assertTrue(readLock.tryLock());
		Assert.assertFalse(writeLock.tryLock());
		readLock.unlock();

		final AtomicInteger callCount = new AtomicInteger(0);
		final Object a = "Object-A";
		final Object b = "Object-B";

		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		callCount.set(0);
		Object o = rwl.readUpgradable(() -> {
			// This should happen with the read lock held
			Assert.assertEquals(1, lock.getReadHoldCount());
			Assert.assertFalse(writeLock.isHeldByCurrentThread());
			callCount.incrementAndGet();
			return a;
		}, (e) -> {
			return true;
		}, (e) -> {
			Assert.fail("This should not have been called");
			return b;
		});
		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertEquals(1, callCount.get());
		Assert.assertSame(o, a);

		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		callCount.set(0);
		o = rwl.readUpgradable(() -> {
			// This should happen with the read lock held
			if (callCount.getAndIncrement() == 0) {
				Assert.assertEquals(1, lock.getReadHoldCount());
				Assert.assertFalse(writeLock.isHeldByCurrentThread());
			} else {
				Assert.assertEquals(0, lock.getReadHoldCount());
				Assert.assertTrue(writeLock.isHeldByCurrentThread());
			}
			return a;
		}, (e) -> {
			return callCount.get() > 1;
		}, (e) -> {
			Assert.fail("This should not have been called");
			return b;
		});
		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertEquals(2, callCount.get());
		Assert.assertSame(o, a);

		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		callCount.set(0);
		o = rwl.readUpgradable(() -> {
			// This should happen with the read lock held
			callCount.incrementAndGet();
			return a;
		}, (e) -> {
			return false;
		}, (e) -> {
			Assert.assertEquals(0, lock.getReadHoldCount());
			Assert.assertTrue(writeLock.isHeldByCurrentThread());
			return b;
		});
		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertEquals(2, callCount.get());
		Assert.assertSame(o, b);

		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		callCount.set(0);
		rwl.readUpgradable(() -> {
			// This should happen with the read lock held
			Assert.assertEquals(1, lock.getReadHoldCount());
			Assert.assertFalse(writeLock.isHeldByCurrentThread());
			callCount.incrementAndGet();
			return a;
		}, (e) -> {
			return true;
		}, (e) -> {
			Assert.fail("This should not have been called");
		});
		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertEquals(1, callCount.get());

		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		callCount.set(0);
		rwl.readUpgradable(() -> {
			// This should happen with the read lock held
			if (callCount.getAndIncrement() == 0) {
				Assert.assertEquals(1, lock.getReadHoldCount());
				Assert.assertFalse(writeLock.isHeldByCurrentThread());
			} else {
				Assert.assertEquals(0, lock.getReadHoldCount());
				Assert.assertTrue(writeLock.isHeldByCurrentThread());
			}
			return a;
		}, (e) -> {
			return callCount.get() > 1;
		}, (e) -> {
			Assert.fail("This should not have been called");
		});
		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertEquals(2, callCount.get());

		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		callCount.set(0);
		rwl.readUpgradable(() -> {
			// This should happen with the read lock held
			callCount.incrementAndGet();
			return a;
		}, (e) -> {
			return false;
		}, (e) -> {
			Assert.assertEquals(0, lock.getReadHoldCount());
			Assert.assertTrue(writeLock.isHeldByCurrentThread());
		});
		Assert.assertEquals(0, lock.getReadHoldCount());
		Assert.assertFalse(writeLock.isHeldByCurrentThread());
		Assert.assertEquals(2, callCount.get());
	}

}