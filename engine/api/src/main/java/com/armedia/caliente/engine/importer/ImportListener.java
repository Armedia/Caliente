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

import com.armedia.caliente.engine.TransferListener;
import com.armedia.caliente.store.CmfObject;

public interface ImportListener extends TransferListener {

	/**
	 * <p>
	 * Invoked when the import of a history of objects has begun, and the estimated number of
	 * objects that will be processed.
	 * </p>
	 *
	 * @param objectType
	 * @param tier
	 */
	public void objectTierImportStarted(UUID jobId, CmfObject.Archetype objectType, int tier);

	/**
	 * <p>
	 * Invoked when the import of a history of objects has begun, and the estimated number of
	 * objects that will be processed.
	 * </p>
	 *
	 * @param objectType
	 * @param historyId
	 * @param count
	 */
	public void objectHistoryImportStarted(UUID jobId, CmfObject.Archetype objectType, String historyId, int count);

	/**
	 * <p>
	 * Invoked when the import has started for the given object.
	 * </p>
	 *
	 * @param object
	 */
	public void objectImportStarted(UUID jobId, CmfObject<?> object);

	/**
	 * <p>
	 * Invoked when the given object has been imported, and indicating the outcome of the import
	 * operation.
	 * </p>
	 *
	 * @param object
	 * @param outcome
	 */
	public void objectImportCompleted(UUID jobId, CmfObject<?> object, ImportOutcome outcome);

	/**
	 * <p>
	 * Invoked when the import attempt on the given object has failed, and indicating the exception
	 * that was raised.
	 * </p>
	 *
	 * @param object
	 * @param thrown
	 */
	public void objectImportFailed(UUID jobId, CmfObject<?> object, Throwable thrown);

	/**
	 * <p>
	 * Invoked when the import of a history has finished.
	 * </p>
	 *
	 * @param objectType
	 * @param historyId
	 * @param outcomes
	 * @param failed
	 */
	public void objectHistoryImportFinished(UUID jobId, CmfObject.Archetype objectType, String historyId,
		Map<String, Collection<ImportOutcome>> outcomes, boolean failed);

	/**
	 * <p>
	 * Invoked when the import of a dependency tier has finished.
	 * </p>
	 *
	 * @param jobId
	 * @param objectType
	 * @param tier
	 * @param failed
	 */
	public void objectTierImportFinished(UUID jobId, CmfObject.Archetype objectType, int tier, boolean failed);
}