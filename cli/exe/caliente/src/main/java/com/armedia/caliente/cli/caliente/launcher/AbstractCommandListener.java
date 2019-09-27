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
package com.armedia.caliente.cli.caliente.launcher;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.armedia.commons.utilities.concurrent.BaseShareableLockable;

class AbstractCommandListener extends BaseShareableLockable {

	protected static final Integer PROGRESS_INTERVAL = 5;

	protected final Logger console;
	protected final CalienteWarningTracker warningTracker;
	protected final AtomicLong progressReporter = new AtomicLong(System.currentTimeMillis());
	private volatile ScheduledExecutorService scheduler = null;
	private ScheduledFuture<?> pingerFuture = null;

	protected AbstractCommandListener(Logger console) {
		this.console = console;
		this.warningTracker = new CalienteWarningTracker(console, true);
	}

	public final CalienteWarningTracker getWarningTracker() {
		return this.warningTracker;
	}

	protected final void startPinger(Runnable r) {
		if (r == null) { return; }
		shareLockedUpgradable(() -> this.scheduler, Objects::isNull, (s) -> {
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			this.pingerFuture = scheduler.scheduleAtFixedRate(r, AbstractCommandListener.PROGRESS_INTERVAL * 3,
				AbstractCommandListener.PROGRESS_INTERVAL, TimeUnit.SECONDS);
			return (this.scheduler = scheduler);
		});
	}

	protected final void stopPinger() {
		shareLockedUpgradable(() -> this.scheduler, Objects::nonNull, (s) -> {
			this.pingerFuture.cancel(true);
			try {
				this.pingerFuture.get();
			} catch (Exception e) {
				// Do nothing...
			} finally {
				this.pingerFuture = null;
				s.shutdownNow();
				this.scheduler = null;
			}
			return null;
		});
	}
}