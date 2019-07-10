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
package com.armedia.caliente.engine.importer;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.armedia.caliente.store.CmfObject;

public class DefaultImportEngineListener extends DefaultImportListener implements ImportEngineListener {

	private final Map<UUID, ImportState> jobStates = new ConcurrentHashMap<>();

	protected final ImportState getState(UUID uuid) {
		if (!this.jobStates.containsKey(uuid)) {
			throw new NoSuchElementException(String.format("Given job ID [%s] has no state stored", uuid.toString()));
		}
		return this.jobStates.get(uuid);
	}

	@Override
	public final void importStarted(ImportState importState, Map<CmfObject.Archetype, Long> summary) {
		if (importState == null) { throw new IllegalArgumentException("Must provide a job import state"); }
		this.jobStates.put(importState.jobId, importState);
		importStartedImpl(importState, summary);

	}

	protected void importStartedImpl(ImportState importState, Map<CmfObject.Archetype, Long> summary) {
	}

	@Override
	public void objectTypeImportStarted(UUID jobId, CmfObject.Archetype objectType, long totalObjects) {
	}

	@Override
	public void objectTypeImportFinished(UUID jobId, CmfObject.Archetype objectType, Map<ImportResult, Long> counters) {
	}

	@Override
	public final void importFinished(UUID jobId, Map<ImportResult, Long> counters) {
		try {
			importFinishedImpl(jobId, counters);
		} finally {
			this.jobStates.remove(jobId);
		}
	}

	protected void importFinishedImpl(UUID jobId, Map<ImportResult, Long> counters) {
	}
}