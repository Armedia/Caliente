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
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceableCondition implements Condition {

	private final Logger log;
	private final Serializable lockId;
	private final String conditionId;
	private final Condition condition;

	public TraceableCondition(Serializable lockId, String conditionId, Condition condition) {
		this(null, lockId, conditionId, condition);
	}

	TraceableCondition(Logger log, Serializable lockId, String conditionId, Condition condition) {
		if (log != null) {
			this.log = log;
		} else {
			this.log = LoggerFactory.getLogger(getClass());
		}
		this.lockId = lockId;
		this.conditionId = conditionId;
		this.condition = condition;
	}

	public final Serializable getLockId() {
		return this.lockId;
	}

	public final String getConditionId() {
		return this.conditionId;
	}

	@Override
	public void await() throws InterruptedException {
		this.log.trace("Lock[{}].Condition[{}].await()", this.lockId, this.conditionId);
		boolean ok = false;
		try {
			this.condition.await();
			ok = true;
		} finally {
			this.log.trace("Lock[{}].Condition[{}].await() {}", this.lockId, this.conditionId,
				ok ? "completed" : "FAILED");
		}
	}

	@Override
	public void awaitUninterruptibly() {
		this.log.trace("Lock[{}].Condition[{}].awaitUninterruptibly()");
		boolean ok = false;
		try {
			this.condition.awaitUninterruptibly();
			ok = true;
		} finally {
			this.log.trace("Lock[{}].Condition[{}].awaitUninterruptibly() {}", this.lockId, this.conditionId,
				ok ? "completed" : "FAILED");
		}
	}

	@Override
	public long awaitNanos(long nanosTimeout) throws InterruptedException {
		this.log.trace("Lock[{}].Condition[{}].awaitNanos({})", this.lockId, this.conditionId, nanosTimeout);
		boolean ok = false;
		Long ret = null;
		try {
			ret = this.condition.awaitNanos(nanosTimeout);
			ok = true;
			return ret;
		} finally {
			this.log.trace("Lock[{}].Condition[{}].awaitNanos({}) {} (returning {})", this.lockId, this.conditionId,
				nanosTimeout, ok ? "completed" : "FAILED", ret);
		}
	}

	@Override
	public boolean await(long time, TimeUnit unit) throws InterruptedException {
		this.log.trace("Lock[{}].Condition[{}].await({}, {})", this.lockId, this.conditionId, time, unit);
		boolean ok = false;
		Boolean ret = null;
		try {
			ret = this.condition.await(time, unit);
			ok = true;
			return ret;
		} finally {
			this.log.trace("Lock[{}].Condition[{}].await({}, {}) {} (returning {})", this.lockId, this.conditionId,
				time, unit, ok ? "completed" : "FAILED", ret);
		}
	}

	@Override
	public boolean awaitUntil(Date deadline) throws InterruptedException {
		this.log.trace("Lock[{}].Condition[{}].awaitUntil({})", this.lockId, this.conditionId, deadline);
		boolean ok = false;
		Boolean ret = null;
		try {
			ret = this.condition.awaitUntil(deadline);
			ok = true;
			return ret;
		} finally {
			this.log.trace("Lock[{}].Condition[{}].awaitUntil({}) {} (returning {})", this.lockId, this.conditionId,
				deadline, ok ? "completed" : "FAILED", ret);
		}
	}

	@Override
	public void signal() {
		this.log.trace("Lock[{}].Condition[{}].signal()", this.lockId, this.conditionId);
		boolean ok = false;
		try {
			this.condition.signal();
			ok = true;
		} finally {
			this.log.trace("Lock[{}].Condition[{}].signal() {}", this.lockId, this.conditionId,
				ok ? "completed" : "FAILED");
		}
	}

	@Override
	public void signalAll() {
		this.log.trace("Lock[{}].Condition[{}].signalAll()", this.lockId, this.conditionId);
		boolean ok = false;
		try {
			this.condition.signalAll();
			ok = true;
		} finally {
			this.log.trace("Lock[{}].Condition[{}].signalAll() {}", this.lockId, this.conditionId,
				ok ? "completed" : "FAILED");
		}
	}
}