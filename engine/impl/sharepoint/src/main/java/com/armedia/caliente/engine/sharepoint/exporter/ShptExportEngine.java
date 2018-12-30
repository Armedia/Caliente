/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.sharepoint.ShptException;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionException;
import com.armedia.caliente.engine.sharepoint.ShptSessionFactory;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.engine.sharepoint.ShptSetting;
import com.armedia.caliente.engine.sharepoint.ShptTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptExportEngine extends
	ExportEngine<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportContextFactory, ShptExportDelegateFactory> {

	public ShptExportEngine(Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Map<String, ?> settings) {
		super(output, warningTracker, baseData, objectStore, contentStore, settings, new CmfCrypt(), false);
	}

	@Override
	protected void findExportResults(ShptSession service, CfgTools configuration, ShptExportDelegateFactory factory,
		TargetSubmitter submitter) throws Exception {
		// support query by path (i.e. all files in these paths)
		// support query by Sharepoint query language
		if (service == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		final String path = configuration.getString(ShptSetting.PATH);
		if (path == null) { throw new ShptException("Must provide the name of the site to export"); }
		final boolean excludeEmptyFolders = configuration.getBoolean(ShptSetting.EXCLUDE_EMPTY_FOLDERS);

		try {
			Iterator<ExportTarget> it = new ShptRecursiveIterator(service, service.getFolder(path), configuration,
				excludeEmptyFolders);
			while (it.hasNext()) {
				submitter.submit(it.next());
			}
		} catch (ShptSessionException e) {
			throw new ShptException("Export target search failed", e);
		}
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return ShptTranslator.INSTANCE;
	}

	@Override
	protected ShptSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new ShptSessionFactory(cfg, crypto);
	}

	@Override
	protected ShptExportContextFactory newContextFactory(ShptSession session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new ShptExportContextFactory(this, cfg, session, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected ShptExportDelegateFactory newDelegateFactory(ShptSession session, CfgTools cfg) throws Exception {
		return new ShptExportDelegateFactory(this, cfg);
	}
}