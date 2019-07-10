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
package com.armedia.caliente.engine.exporter;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.armedia.caliente.store.CmfObject;

public class DefaultExportEngineListener extends DefaultExportListener implements ExportEngineListener {

	private final Map<UUID, ExportState> jobStates = new ConcurrentHashMap<>();

	protected final ExportState getState(UUID uuid) {
		if (uuid == null) { throw new NullPointerException("Must provide a job ID to find"); }
		if (!this.jobStates.containsKey(uuid)) {
			throw new NoSuchElementException(String.format("Given job ID [%s] has no state stored", uuid.toString()));
		}
		return this.jobStates.get(uuid);
	}

	@Override
	public final void exportStarted(ExportState exportState) {
		if (exportState == null) { throw new IllegalArgumentException("Must provide a job export state"); }
		this.jobStates.put(exportState.jobId, exportState);
		exportStartedImpl(exportState);
	}

	protected void exportStartedImpl(ExportState exportState) {
		// Do whatever...
	}

	@Override
	public void exportFinished(UUID jobId, Map<CmfObject.Archetype, Long> summary) {
		try {
			exportFinishedImpl(jobId, summary);
		} finally {
			this.jobStates.remove(jobId);
		}
	}

	protected void exportFinishedImpl(UUID jobId, Map<CmfObject.Archetype, Long> summary) {
	}

	@Override
	public void sourceSearchStarted(String source) {
	}

	@Override
	public void sourceSearchMilestone(String source, long sourceCount, long totalCount) {
	}

	@Override
	public void sourceSearchCompleted(String source, long sourceCount, long totalCount) {
	}

	@Override
	public void sourceSearchFailed(String source, long sourceCount, long totalCount, Exception thrown) {
	}

	@Override
	public void searchCompleted(long totalCount) {
	}

	@Override
	public void searchFailed(long totalCount, Exception thrown) {
	}
}