package com.delta.cmsmf.launcher;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;

import com.armedia.cmf.engine.exporter.DefaultExportEngineListener;
import com.armedia.cmf.engine.exporter.ExportResult;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class ExportManifest extends DefaultExportEngineListener {

	private final Logger manifestLog = Logger.getLogger("manifest");

	private static final String RECORD_FORMAT = "%s,%s,%s,%s,%s,%s,%s";

	private static final class Record {
		private final String date;
		private final CmfType type;
		private final String batchId;
		private final String sourceId;
		private final String label;
		private final ExportResult result;
		private final Throwable thrown;

		private Record(CmfObject<?> object, Throwable thrown) {
			this(object, ExportResult.FAILED, thrown);
		}

		private Record(CmfObject<?> object, ExportResult result) {
			this(object, result, null);
		}

		private Record(CmfObject<?> object, ExportResult result, Throwable thrown) {
			this.date = StringEscapeUtils.escapeCsv(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date()));
			this.type = object.getType();
			this.batchId = StringEscapeUtils.escapeCsv(object.getBatchId());
			this.sourceId = StringEscapeUtils.escapeCsv(object.getId());
			this.label = StringEscapeUtils.escapeCsv(object.getLabel());
			this.result = result;
			if (result != ExportResult.FAILED) {
				this.thrown = null;
			} else {
				this.thrown = thrown;
				if (thrown == null) { throw new IllegalArgumentException("Must provide a reason for the failure"); }
			}
		}

		private Record(CmfType type, String objectId, Throwable thrown) {
			this(type, objectId, ExportResult.FAILED, thrown);

		}

		private Record(CmfType type, String objectId, ExportResult result) {
			this(type, objectId, result, null);
		}

		private Record(CmfType type, String objectId, ExportResult result, Throwable thrown) {
			this.date = StringEscapeUtils.escapeCsv(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date()));
			this.type = type;
			this.batchId = "";
			this.sourceId = StringEscapeUtils.escapeCsv(objectId);
			this.label = "";
			this.result = result;
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
			return StringEscapeUtils.escapeCsv(base);
		}

		public void log(Logger log) {
			final String msg;
			if (this.result != ExportResult.FAILED) {
				msg = String.format(ExportManifest.RECORD_FORMAT, this.date, this.type.name(), this.result.name(),
					this.batchId, this.sourceId, this.label, "");
			} else {
				msg = String.format(ExportManifest.RECORD_FORMAT, this.date, this.type.name(), this.result.name(),
					this.batchId, this.sourceId, this.label, getThrownMessage());
			}
			log.info(msg);
		}
	}

	private final Map<String, List<Record>> openBatches = new ConcurrentHashMap<String, List<Record>>();
	private final Set<ExportResult> results;
	private final Set<CmfType> types;

	public ExportManifest(Set<ExportResult> results, Set<CmfType> types) {
		this.results = Tools.freezeCopy(results, true);
		this.types = Tools.freezeCopy(types, true);
	}

	@Override
	public void exportStarted(CfgTools config) {
		this.openBatches.clear();
		this.manifestLog.info(String.format(ExportManifest.RECORD_FORMAT, "DATE", "TYPE", "RESULT", "BATCH_ID",
			"SOURCE_ID", "LABEL", "ERROR_DATA"));
	}

	@Override
	public void objectExportCompleted(CmfObject<?> object, Long objectNumber) {
		if (!this.types.contains(object.getType())) { return; }
		if (!this.results.contains(ExportResult.EXPORTED)) { return; }
		new Record(object, ExportResult.EXPORTED).log(this.manifestLog);
	}

	@Override
	public void objectSkipped(CmfType objectType, String objectId) {
		// For the manifest, we're not really interested in Skipped objects, since
		// they'll always be the result of duplicate serializations, so there's no
		// problem to be reported or deduced from it
		if (!this.types.contains(objectType)) { return; }
		if (!this.results.contains(ExportResult.SKIPPED)) { return; }
		new Record(objectType, objectId, ExportResult.SKIPPED).log(this.manifestLog);
	}

	@Override
	public void objectExportFailed(CmfType objectType, String objectId, Throwable thrown) {
		if (!this.types.contains(objectType)) { return; }
		if (!this.results.contains(ExportResult.FAILED)) { return; }
		new Record(objectType, objectId, thrown).log(this.manifestLog);
	}
}