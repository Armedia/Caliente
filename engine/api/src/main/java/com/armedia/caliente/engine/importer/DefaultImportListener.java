/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfObject;

public class DefaultImportListener implements ImportListener {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void objectTierImportStarted(UUID jobId, CmfObject.Archetype objectType, int tier) {
	}

	@Override
	public void objectHistoryImportStarted(UUID jobId, CmfObject.Archetype objectType, String batchId, int count) {
	}

	@Override
	public void objectImportStarted(UUID jobId, CmfObject<?> object) {
	}

	@Override
	public void objectImportCompleted(UUID jobId, CmfObject<?> object, ImportOutcome outcome) {
	}

	@Override
	public void objectImportFailed(UUID jobId, CmfObject<?> object, Throwable thrown) {
	}

	@Override
	public void objectHistoryImportFinished(UUID jobId, CmfObject.Archetype objectType, String batchId,
		Map<String, Collection<ImportOutcome>> outcomes, boolean failed) {
	}

	@Override
	public void objectTierImportFinished(UUID jobId, CmfObject.Archetype objectType, int tier, boolean failed) {
	}
}