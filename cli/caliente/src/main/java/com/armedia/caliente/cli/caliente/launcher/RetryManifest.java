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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.exporter.DefaultExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportSkipReason;
import com.armedia.caliente.engine.exporter.ExportState;
import com.armedia.caliente.engine.importer.ImportRestriction;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectSearchSpec;
import com.armedia.caliente.tools.CsvFormatter;
import com.armedia.commons.utilities.Tools;

/**
 *
 *
 */
public class RetryManifest extends DefaultExportEngineListener {

	private final Logger retriesLog = LoggerFactory.getLogger("retries");

	private static final CsvFormatter FORMAT = new CsvFormatter( //
		"RETRY_ID" //
	);

	private static final class Record {
		private final String retryId;

		private Record(CmfObjectSearchSpec spec) {
			this.retryId = ImportRestriction.render(spec);
		}

		public void log(Logger log) {
			final String msg = RetryManifest.FORMAT.render( //
				this.retryId //
			);
			log.info(msg);
		}
	}

	private final Map<String, List<Record>> openBatches = new ConcurrentHashMap<>();
	private final Set<CmfObject.Archetype> types;

	public RetryManifest(Set<CmfObject.Archetype> types) {
		this.types = Tools.freezeCopy(types, true);
	}

	@Override
	protected void exportStartedImpl(ExportState exportState) {
		this.openBatches.clear();
	}

	@Override
	public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber) {
	}

	@Override
	public void objectSkipped(UUID jobId, CmfObjectSearchSpec object, ExportSkipReason reason, String extraInfo) {
	}

	@Override
	public void objectExportFailed(UUID jobId, CmfObjectSearchSpec object, Throwable thrown) {
		if (!this.types.contains(object.getType())) { return; }
		new Record(object).log(this.retriesLog);
	}
}