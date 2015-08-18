package com.delta.cmsmf.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;

import com.armedia.cmf.engine.importer.DefaultImportEngineListener;
import com.armedia.cmf.engine.importer.ImportOutcome;
import com.armedia.cmf.engine.importer.ImportResult;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.Tools;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class ImportManifest extends DefaultImportEngineListener {

	private final Logger manifestLog = Logger.getLogger("manifest");

	private static final String RECORD_FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s";

	private static final class Record {
		private final String date;
		private final StoredObjectType type;
		private final String batchId;
		private final String sourceId;
		private final String label;
		private final String targetId;
		private final ImportResult result;
		private final Throwable thrown;

		private Record(StoredObject<?> object, Throwable thrown) {
			this(object, null, ImportResult.FAILED, thrown);
		}

		private Record(StoredObject<?> object, String targetId, ImportResult result) {
			this(object, targetId, result, null);
		}

		private Record(StoredObject<?> object, String targetId, ImportResult result, Throwable thrown) {
			this.date = StringEscapeUtils.escapeCsv(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date()));
			this.type = object.getType();
			this.batchId = StringEscapeUtils.escapeCsv(object.getBatchId());
			this.sourceId = StringEscapeUtils.escapeCsv(object.getId());
			this.label = StringEscapeUtils.escapeCsv(object.getLabel());
			this.result = result;
			if (result != ImportResult.FAILED) {
				this.targetId = StringEscapeUtils.escapeCsv(Tools.coalesce(targetId, ""));
				this.thrown = null;
			} else {
				this.targetId = StringEscapeUtils.escapeCsv("");
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
			if (this.result != ImportResult.FAILED) {
				msg = String.format(ImportManifest.RECORD_FORMAT, this.date, this.type.name(), this.result.name(),
					this.batchId, this.sourceId, this.targetId, this.label, "");
			} else {
				msg = String.format(ImportManifest.RECORD_FORMAT, this.date, this.type.name(), this.result.name(),
					this.batchId, this.sourceId, this.targetId, this.label, getThrownMessage());
			}
			log.info(msg);
		}
	}

	private final Map<String, List<Record>> openBatches = new ConcurrentHashMap<String, List<Record>>();
	private final Set<ImportResult> results;
	private final Set<StoredObjectType> types;

	public ImportManifest(Set<ImportResult> results, Set<StoredObjectType> types) {
		this.results = Tools.freezeCopy(results, true);
		this.types = Tools.freezeCopy(types, true);
	}

	@Override
	public void importStarted(Map<StoredObjectType, Integer> summary) {
		// Clear manifest
		this.openBatches.clear();
		this.manifestLog.info(String.format(ImportManifest.RECORD_FORMAT, "DATE", "TYPE", "RESULT", "BATCH_ID",
			"SOURCE_ID", "TARGET_ID", "LABEL", "ERROR_DATA"));
	}

	@Override
	public void objectImportStarted(StoredObject<?> object) {
		// TODO Auto-generated method stub
		super.objectImportStarted(object);
	}

	@Override
	public void objectBatchImportStarted(StoredObjectType objectType, String batchId, int count) {
		if (!this.types.contains(objectType)) { return; }
		if (count <= 1) {
			// We don't track batches with a single item because it's not worth the trouble
			// This also covers the case when batch's contents are parallelized, but batches
			// themselves are serialized (like for Folders or Types)
			return;
		}
		this.openBatches.put(batchId, Collections.synchronizedList(new ArrayList<Record>(count)));
	}

	@Override
	public void objectImportCompleted(StoredObject<?> object, ImportOutcome outcome) {
		if (!this.types.contains(object.getType())) { return; }
		if (!this.results.contains(outcome.getResult())) { return; }
		Record record = new Record(object, outcome.getNewId(), outcome.getResult());
		List<Record> batch = this.openBatches.get(object.getBatchId());
		if (batch != null) {
			batch.add(record);
		} else {
			// If this is a single object, we log it right away
			record.log(this.manifestLog);
		}
	}

	@Override
	public void objectImportFailed(StoredObject<?> object, Throwable thrown) {
		if (!this.types.contains(object.getType())) { return; }
		if (!this.results.contains(ImportResult.FAILED)) { return; }
		Record record = new Record(object, thrown);
		List<Record> batch = this.openBatches.get(object.getBatchId());
		if (batch != null) {
			batch.add(record);
		} else {
			// If this is a single object, we log it right away
			record.log(this.manifestLog);
		}
	}

	@Override
	public void objectBatchImportFinished(StoredObjectType objectType, String batchId,
		Map<String, ImportOutcome> outcomes, boolean failed) {
		if (!this.types.contains(objectType)) { return; }
		List<Record> batch = this.openBatches.get(batchId);
		if (batch != null) {
			// output each record in (roughly) the order they were imported
			for (Record r : batch) {
				r.log(this.manifestLog);
			}
		}
	}

	@Override
	public void importFinished(Map<ImportResult, Integer> counters) {
		// We're done...
	}
}