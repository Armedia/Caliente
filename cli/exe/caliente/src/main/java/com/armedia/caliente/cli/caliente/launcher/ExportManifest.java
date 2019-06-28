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
package com.armedia.caliente.cli.caliente.launcher;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.exporter.DefaultExportEngineListener;
import com.armedia.caliente.engine.exporter.ExportResult;
import com.armedia.caliente.engine.exporter.ExportSkipReason;
import com.armedia.caliente.engine.exporter.ExportState;
import com.armedia.caliente.engine.importer.ImportRestriction;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectSearchSpec;
import com.armedia.commons.utilities.Tools;

/**
 *
 *
 */
public class ExportManifest extends DefaultExportEngineListener {

	private final Logger manifestLog = LoggerFactory.getLogger("manifest");

	private static final Long NULL = Long.valueOf(-1);

	private static final ManifestFormatter FORMAT = new ManifestFormatter("NUMBER", //
		"DATE", //
		"TYPE", //
		"TIER", //
		"RESULT", //
		"SEARCH_KEY", //
		"RETRY_ID", //
		"HISTORY_ID", //
		"SOURCE_ID", //
		"LABEL", //
		"ERROR_DATA" //
	);

	private static final class Record {
		private final Long number;
		private final String date;
		private final CmfObject.Archetype type;
		private final Integer tier;
		private final String searchKey;
		private final String retryId;
		private final String historyId;
		private final String sourceId;
		private final String label;
		private final ExportResult result;
		private final Throwable thrown;
		private final String extraInfo;

		private Record(CmfObject<?> object, Throwable thrown) {
			this(object, ExportResult.FAILED, thrown, null);
		}

		private Record(CmfObject<?> object, ExportResult result) {
			this(object, result, null, null);
		}

		private Record(CmfObject<?> object, ExportResult result, String extraInfo) {
			this(object, result, null, extraInfo);
		}

		private Record(CmfObject<?> object, ExportResult result, Throwable thrown) {
			this(object, result, thrown, null);
		}

		private Record(CmfObject<?> object, ExportResult result, Throwable thrown, String extraInfo) {
			this.number = object.getNumber();
			this.date = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(new Date());
			this.type = object.getType();
			this.tier = object.getDependencyTier();
			this.searchKey = String.format("%%%s", object.getSearchKey(), object.getId());
			this.retryId = ImportRestriction.render(object);
			this.historyId = object.getHistoryId();
			this.sourceId = object.getId();
			this.label = object.getLabel();
			this.result = result;
			this.extraInfo = extraInfo;
			if (result != ExportResult.FAILED) {
				this.thrown = null;
			} else {
				this.thrown = thrown;
				if (thrown == null) { throw new IllegalArgumentException("Must provide a reason for the failure"); }
			}
		}

		private Record(CmfObjectSearchSpec spec, Throwable thrown) {
			this(spec, ExportResult.FAILED, thrown, null);

		}

		private Record(CmfObjectSearchSpec spec, ExportResult result, String extraInfo) {
			this(spec, result, null, extraInfo);
		}

		private Record(CmfObjectSearchSpec spec, ExportResult result, Throwable thrown, String extraInfo) {
			this.number = ExportManifest.NULL;
			this.date = DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(new Date());
			this.type = spec.getType();
			this.tier = null;
			this.searchKey = String.format("%%%s", spec.getSearchKey());
			this.retryId = ImportRestriction.render(spec);
			this.historyId = "";
			this.sourceId = spec.getId();
			this.label = "";
			this.result = result;
			this.extraInfo = extraInfo;
			if (result != ExportResult.FAILED) {
				this.thrown = null;
			} else {
				this.thrown = thrown;
				if (thrown == null) { throw new IllegalArgumentException("Must provide a reason for the failure"); }
			}
		}

		private String formatThrown(Throwable t) {
			String msg = t.getMessage();
			return String.format("%s%s%s", t.getClass().getCanonicalName(), StringUtils.isBlank(msg) ? "" : ": ",
				Tools.coalesce(msg, ""));
		}

		private String getThrownMessage() {
			String base = formatThrown(this.thrown);
			Throwable cause = this.thrown.getCause();
			if (cause != null) {
				base = String.format("%s (%s)", base, formatThrown(cause));
			}
			return base;
		}

		public void log(Logger log) {
			final String extraData = (this.result != ExportResult.FAILED) ? Tools.coalesce(this.extraInfo, "")
				: getThrownMessage();
			final String msg = ExportManifest.FORMAT.render( //
				this.number, //
				this.date, //
				this.type.name(), //
				(this.tier != null ? this.tier.toString() : ""), //
				this.result.name(), //
				this.searchKey, //
				this.retryId, //
				this.historyId, //
				this.sourceId, //
				this.label, //
				extraData //
			);
			log.info(msg);
		}
	}

	private final Map<String, List<Record>> openBatches = new ConcurrentHashMap<>();
	private final Set<ExportResult> results;
	private final Set<CmfObject.Archetype> types;

	public ExportManifest(Set<ExportResult> results, Set<CmfObject.Archetype> types) {
		this.results = Tools.freezeCopy(results, true);
		this.types = Tools.freezeCopy(types, true);
	}

	@Override
	protected void exportStartedImpl(ExportState exportState) {
		this.openBatches.clear();
		this.manifestLog.info(ExportManifest.FORMAT.renderHeaders());
	}

	@Override
	public void objectExportCompleted(UUID jobId, CmfObject<?> object, Long objectNumber) {
		if (!this.types.contains(object.getType())) { return; }
		if (!this.results.contains(ExportResult.EXPORTED)) { return; }
		new Record(object, ExportResult.EXPORTED).log(this.manifestLog);
	}

	@Override
	public void objectSkipped(UUID jobId, CmfObjectSearchSpec object, ExportSkipReason reason, String extraInfo) {
		// For the manifest, we're not really interested in Skipped objects, since
		// they'll always be the result of duplicate serializations, so there's no
		// problem to be reported or deduced from it
		if (!this.types.contains(object.getType())) { return; }
		if (!this.results.contains(ExportResult.SKIPPED)) { return; }
		switch (reason) {
			case SKIPPED:
			case UNSUPPORTED:
			case DEPENDENCY_FAILED:
				new Record(object, ExportResult.SKIPPED, extraInfo).log(this.manifestLog);
				break;
			default:
				break;
		}
	}

	@Override
	public void objectExportFailed(UUID jobId, CmfObjectSearchSpec object, Throwable thrown) {
		if (!this.types.contains(object.getType())) { return; }
		if (!this.results.contains(ExportResult.FAILED)) { return; }
		new Record(object, thrown).log(this.manifestLog);
	}
}