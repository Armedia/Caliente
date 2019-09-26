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
package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;
import java.util.concurrent.locks.ReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.ShareableLockable;

public abstract class ContentPersistor extends BaseShareableLockable implements AutoCloseable {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final Logger error = LoggerFactory.getLogger("errors");
	protected final Logger console = LoggerFactory.getLogger("console");

	private boolean initialized = false;

	protected final File target;

	protected ContentPersistor(File target) {
		this.target = Tools.canonicalize(target);
	}

	public ContentPersistor(ReadWriteLock rwLock, File target) {
		super(rwLock);
		this.target = Tools.canonicalize(target);
	}

	public ContentPersistor(ShareableLockable lockable, File target) {
		super(lockable);
		this.target = Tools.canonicalize(target);
	}

	public final void initialize() throws Exception {
		try (MutexAutoLock lock = autoMutexLock()) {
			if (this.initialized) {
				throw new IllegalStateException("This persistor is already initialized!! Close it first!");
			}
			try {
				startup();
				this.initialized = true;
			} finally {
				if (!this.initialized) {
					cleanup();
				}
			}
		}
	}

	protected abstract void startup() throws Exception;

	public final void persist(Content content) {
		if (content == null) { return; }
		try {
			mutexLocked(() -> persistContent(content));
		} catch (Exception e) {
			this.error.error("Failed to persist the content object {}", content, e);
		}
	}

	protected abstract void persistContent(Content content) throws Exception;

	@Override
	public final void close() {
		try (MutexAutoLock lock = autoMutexLock()) {
			if (!this.initialized) { return; }
			try {
				cleanup();
			} finally {
				this.initialized = false;
			}
		} catch (Exception e) {
			this.error.warn("Failed to close this persistor", e);
		}
	}

	protected abstract void cleanup() throws Exception;
}