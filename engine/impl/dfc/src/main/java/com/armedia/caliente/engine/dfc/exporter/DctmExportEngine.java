/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmSessionFactory;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.DctmTranslator;
import com.armedia.caliente.engine.dfc.UnsupportedDctmObjectTypeException;
import com.armedia.caliente.engine.dfc.common.Setting;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportResultSubmitter;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.caliente.tools.ResourceLoader;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.CloseableIterator;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportEngine extends
	ExportEngine<IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportContextFactory, DctmExportDelegateFactory, DctmExportEngineFactory> {

	public DctmExportEngine(DctmExportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings);
	}

	@Override
	protected void findExportResults(IDfSession session, CfgTools configuration, DctmExportDelegateFactory factory,
		ExportResultSubmitter submitter) throws Exception {
		if (session == null) {
			throw new IllegalArgumentException("Must provide a session through which to retrieve the results");
		}
		String source = configuration.getString(Setting.SOURCE);
		if (source == null) { throw new Exception("Must provide the DQL to query with"); }

		// Remove any leading spaces - trailing spaces may be of significance
		source = StringUtils.stripStart(source, null);

		if (source.startsWith("@")) {
			// This is either a path or a URL to a file listing IDs to be retrieved
			source = source.substring(1); // Remove the leading @

			URL url = ResourceLoader.getResourceOrFile(source);
			if (url == null) {
				throw new Exception(String.format("Failed to find the object ID list at [%s]", source));
			}
			try (InputStream in = url.openStream()) {
				try (Reader r = new InputStreamReader(in)) {
					try (LineNumberReader lin = new LineNumberReader(r)) {
						while (true) {
							final String rawLine = lin.readLine();
							if (rawLine == null) { return; }

							// Remove everything past the first #
							String line = rawLine.replaceAll("\\s*#.*$", "");
							line = StringUtils.strip(line);
							if (StringUtils.isBlank(line)) {
								// Ignore empty lines
								continue;
							}

							// This is an object ID, just create an ExportTarget for it
							IDfId id = new DfId(line);
							if (id.isNull()) {
								// Ignore the null ID
								this.log.warn("NULL_ID ignored on line {} : [{}]", lin.getLineNumber(), rawLine);
								continue;
							}

							ExportTarget target = null;
							DctmObjectType type = DctmObjectType.decodeType(id);
							if (type != null) {
								target = new ExportTarget(type.getStoredObjectType(), id.getId(), id.getId());
							} else {
								try {
									target = DctmExportTools.getExportTarget(session, id, null);
								} catch (UnsupportedDctmObjectTypeException e) {
									this.log.warn("Unsupported object type for ID [{}] on line {} : [{}]", id,
										lin.getLineNumber(), rawLine);
								}
							}
							submitter.submit(target);
						}
					}
				}
			}
		}

		if (source.startsWith("#") || source.startsWith("/")) {
			final char marker = source.charAt(0);
			final IDfPersistentObject object;
			final IDfId id;

			if (marker == '#') {
				// This is an object ID, so work it...
				source = source.substring(1); // Remove the leading marker
				source = StringUtils.strip(source);
				id = new DfId(source);
				if (!id.isObjectId()) {
					throw new Exception(String.format("Bad ID given (%s), nothing will be exported", source));
				}

				if (id.isNull()) {
					this.log.warn("Null ID given ({}), nothing will be exported", id);
					return;
				}

				object = session.getObject(id);
				if (object == null) {
					throw new Exception(String.format("Failed to find any objects with the ID [%s]", source));
				}
			} else {
				// This is a path, so find the object by path and export everything within it...
				object = session.getObjectByPath(source);
				if (object == null) {
					throw new Exception(String.format("Failed to find any objects at the path [%s]", source));
				}

				id = object.getObjectId();
			}

			final DctmObjectType type;
			try {
				type = DctmObjectType.decodeType(object);
			} catch (UnsupportedDctmObjectTypeException e) {
				throw new Exception(
					String.format("The object at the path [%s] is of an unsupported object type", source), e);
			}

			submitter.submit(DctmExportTools.getExportTarget(object));
			if (type != DctmObjectType.FOLDER) { return; }

			// Change the source into a DQL predicate, and let the rest of the code take it from
			// there...
			source = String.format("dm_sysobject where folder(ID(%s), DESCEND)", DfUtils.quoteString(id.getId()));
		}

		// This must be a predicate, so turn it into a DQL query
		source = StringUtils.strip(source);
		source = String.format("select r_object_id from %s", source);

		final int batchSize = configuration.getInteger(Setting.EXPORT_BATCH_SIZE);

		try (CloseableIterator<ExportTarget> it = new DctmExportTargetIterator(
			DfUtils.executeQuery(session, source.toString(), IDfQuery.DF_EXECREAD_QUERY, batchSize))) {
			while (it.hasNext()) {
				submitter.submit(it.next());
			}
		}
	}

	@Override
	public CmfAttributeTranslator<IDfValue> getTranslator() {
		return DctmTranslator.INSTANCE;
	}

	@Override
	protected SessionFactory<IDfSession> newSessionFactory(CfgTools config, CmfCrypt crypto) throws Exception {
		return new DctmSessionFactory(config, crypto);
	}

	@Override
	protected DctmExportContextFactory newContextFactory(IDfSession session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new DctmExportContextFactory(this, cfg, session, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected DctmExportDelegateFactory newDelegateFactory(IDfSession session, CfgTools cfg) throws Exception {
		return new DctmExportDelegateFactory(this, cfg);
	}

	@Override
	protected IDfValue getValue(CmfDataType type, Object value) {
		return DfValueFactory.newValue(DctmTranslator.translateType(type).getDfConstant(), value);
	}
}