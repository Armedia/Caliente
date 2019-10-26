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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceableCondition implements Condition, Serializable, Traceable {
	private static final long serialVersionUID = 1L;

	private final Logger log;
	private final String name;
	private final Serializable id;
	private final Condition condition;

	public TraceableCondition(String lockName, Serializable id, Condition condition) {
		this(null, lockName, id, condition);
	}

	TraceableCondition(Logger log, String lockName, Serializable id, Condition condition) {
		this.id = Objects.requireNonNull(id, "Must provide a non-null ID");
		this.name = Traceable.format("{}.condition[{}]",
			Objects.requireNonNull(lockName, "Must provide the name of the owning lock"), id);
		this.condition = Objects.requireNonNull(condition, "Must provide a non-null Condition");
		if (log != null) {
			this.log = log;
		} else {
			this.log = LoggerFactory.getLogger(getClass());
		}
	}

	@Override
	public Logger getLog() {
		return this.log;
	}

	@Override
	public Serializable getId() {
		return this.id;
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public void await() throws InterruptedException {
		invoke(() -> this.condition.await(), "await");
	}

	@Override
	public void awaitUninterruptibly() {
		invoke(this.condition::awaitUninterruptibly, "awaitUninterruptibly");
	}

	@Override
	public long awaitNanos(long nanosTimeout) throws InterruptedException {
		return invoke(() -> this.condition.awaitNanos(nanosTimeout), "awaitNanos", nanosTimeout);
	}

	@Override
	public boolean await(long time, TimeUnit unit) throws InterruptedException {
		return invoke(() -> this.condition.await(time, unit), "await", time, unit);
	}

	@Override
	public boolean awaitUntil(Date deadline) throws InterruptedException {
		return invoke(() -> this.condition.awaitUntil(deadline), "awaitUntil", deadline);
	}

	@Override
	public void signal() {
		invoke(this.condition::signal, "signal");
	}

	@Override
	public void signalAll() {
		invoke(this.condition::signalAll, "signalAll");
	}
}