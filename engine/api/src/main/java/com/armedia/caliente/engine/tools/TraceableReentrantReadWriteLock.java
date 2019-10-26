/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.tools;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceableReentrantReadWriteLock extends ReentrantReadWriteLock implements Traceable {
	private static final long serialVersionUID = 1L;

	private static final AtomicLong LOCK_COUNTER = new AtomicLong(0);

	private class TraceableReadLock extends ReadLock implements Traceable {
		private static final long serialVersionUID = 1L;

		private final String name;

		private TraceableReadLock(ReentrantReadWriteLock lock) {
			super(lock);
			this.name = Traceable.format("{}.ReadLock", TraceableReentrantReadWriteLock.this.name);
			TraceableReentrantReadWriteLock.this.log.trace("{}.constructed()", this.name);
		}

		@Override
		public Logger getLog() {
			return TraceableReentrantReadWriteLock.this.log;
		}

		@Override
		public Serializable getId() {
			return TraceableReentrantReadWriteLock.this.id;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public void lock() {
			invoke(super::lock, "lock");
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			invoke(super::lockInterruptibly, "lockInterruptibly");
		}

		@Override
		public boolean tryLock() {
			return invoke(() -> super.tryLock(), "tryLock");
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			return invoke(() -> super.tryLock(time, unit), "tryLock", time, unit);
		}

		@Override
		public void unlock() {
			invoke(super::unlock, "unlock");
		}

		@Override
		public Condition newCondition() {
			final Condition condition = super.newCondition();
			final String conditionId = String.format("%016x",
				TraceableReentrantReadWriteLock.this.conditionCounter.getAndIncrement());
			return new TraceableCondition(TraceableReentrantReadWriteLock.this.log, this.name, conditionId, condition);
		}
	}

	private class TraceableWriteLock extends WriteLock implements Traceable {

		private static final long serialVersionUID = 1L;

		private final String name;

		private TraceableWriteLock(ReentrantReadWriteLock lock) {
			super(lock);
			this.name = Traceable.format("{}.WriteLock", TraceableReentrantReadWriteLock.this.name);
			TraceableReentrantReadWriteLock.this.log.trace("{}.constructed()", this.name);
		}

		@Override
		public Logger getLog() {
			return TraceableReentrantReadWriteLock.this.log;
		}

		@Override
		public Serializable getId() {
			return TraceableReentrantReadWriteLock.this.id;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public void lock() {
			invoke(super::lock, "lock");
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			invoke(super::lockInterruptibly, "lockInterruptibly");
		}

		@Override
		public boolean tryLock() {
			return invoke(() -> super.tryLock(), "tryLock");
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			return invoke(() -> super.tryLock(time, unit), "tryLock", time, unit);
		}

		@Override
		public void unlock() {
			invoke(super::unlock, "unlock");
		}

		@Override
		public Condition newCondition() {
			final Condition condition = super.newCondition();
			final String conditionId = String.format("%016x",
				TraceableReentrantReadWriteLock.this.conditionCounter.getAndIncrement());
			return new TraceableCondition(TraceableReentrantReadWriteLock.this.log, this.name, conditionId, condition);
		}
	}

	private final AtomicLong conditionCounter = new AtomicLong(0);
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Serializable id;
	private final String name;
	private final TraceableReadLock readLock;
	private final TraceableWriteLock writeLock;

	public TraceableReentrantReadWriteLock() {
		this(false);
	}

	public TraceableReentrantReadWriteLock(boolean fair) {
		this(String.format("%016x", TraceableReentrantReadWriteLock.LOCK_COUNTER.getAndIncrement()), fair);
	}

	public TraceableReentrantReadWriteLock(Serializable lockId) {
		this(lockId, false);
	}

	public TraceableReentrantReadWriteLock(Serializable lockId, boolean fair) {
		super(fair);
		this.id = lockId;
		this.name = Traceable.format("ReentrantReadWriteLock[{}]", this.id);
		this.readLock = new TraceableReadLock(this);
		this.writeLock = new TraceableWriteLock(this);
		this.log.trace("{}.constructed()", this.name);
	}

	@Override
	public Logger getLog() {
		return this.log;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public final Serializable getId() {
		return this.id;
	}

	@Override
	public ReadLock readLock() {
		return this.readLock;
	}

	@Override
	public WriteLock writeLock() {
		return this.writeLock;
	}
}