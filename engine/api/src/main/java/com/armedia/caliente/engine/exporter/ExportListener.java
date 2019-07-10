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

import java.util.UUID;

import com.armedia.caliente.engine.TransferListener;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectSearchSpec;

public interface ExportListener extends TransferListener {

	/**
	 * <p>
	 * Invoked when export has started for the given object.
	 * </p>
	 */
	public void objectExportStarted(UUID jobId, CmfObjectSearchSpec object, CmfObjectSearchSpec referrent);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 */
	public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber);

	/**
	 * <p>
	 * Invoked when the given object has been skipped.
	 * </p>
	 */
	public void objectSkipped(UUID jobId, CmfObjectSearchSpec object, ExportSkipReason reason, String extraInfo);

	/**
	 * <p>
	 * Invoked when the given object has been exported.
	 * </p>
	 */
	public void objectExportFailed(UUID jobId, CmfObjectSearchSpec object, Throwable thrown);

	/**
	 * <p>
	 * Invoked when a data consistency issue has been encountered, so it can be reported and tracked
	 * </p>
	 */
	public void consistencyWarning(UUID jobId, CmfObjectSearchSpec object, String fmt, Object... args);
}