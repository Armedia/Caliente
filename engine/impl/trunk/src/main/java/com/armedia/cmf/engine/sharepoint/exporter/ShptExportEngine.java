/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.armedia.cmf.engine.TransferEngine;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.Setting;
import com.armedia.cmf.engine.sharepoint.ShptException;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionException;
import com.armedia.cmf.engine.sharepoint.ShptSessionFactory;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.engine.sharepoint.ShptTranslator;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptExportEngine
	extends
	ExportEngine<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportContextFactory, ShptExportDelegateFactory> {

	private static final Set<String> TARGETS = Collections.singleton(ShptObject.TARGET_NAME);

	@Override
	protected Iterator<ExportTarget> findExportResults(ShptSession service, CfgTools configuration,
		ShptExportDelegateFactory factory) throws Exception {
		// support query by path (i.e. all files in these paths)
		// support query by Sharepoint query language
		if (service == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		final String path = configuration.getString(Setting.PATH);
		if (path == null) { throw new ShptException("Must provide the name of the site to export"); }
		final boolean excludeEmptyFolders = configuration.getBoolean(Setting.EXCLUDE_EMPTY_FOLDERS);

		try {
			return new ShptRecursiveIterator(service, service.getFolder(path), configuration, excludeEmptyFolders);
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
	protected ShptSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new ShptSessionFactory(cfg);
	}

	@Override
	protected ShptExportContextFactory newContextFactory(ShptSession session, CfgTools cfg) throws Exception {
		return new ShptExportContextFactory(this, cfg);
	}

	@Override
	protected ShptExportDelegateFactory newDelegateFactory(ShptSession session, CfgTools cfg) throws Exception {
		return new ShptExportDelegateFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return ShptExportEngine.TARGETS;
	}

	public static ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return TransferEngine.getTransferEngine(ExportEngine.class, ShptExportEngine.TARGETS.iterator().next());
	}
}