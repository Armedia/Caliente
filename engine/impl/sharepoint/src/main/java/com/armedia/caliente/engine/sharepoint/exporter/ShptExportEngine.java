/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.Collections;
import java.util.Set;

import com.armedia.caliente.engine.CmfCrypt;
import com.armedia.caliente.engine.TransferEngine;
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
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.CloseableIteratorWrapper;

/**
 * @author diego
 *
 */
public class ShptExportEngine extends
	ExportEngine<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportContextFactory, ShptExportDelegateFactory> {

	public ShptExportEngine() {
		super(new CmfCrypt());
	}

	private static final Set<String> TARGETS = Collections.singleton(ShptObject.TARGET_NAME);

	@Override
	protected CloseableIterator<ExportTarget> findExportResults(ShptSession service, CfgTools configuration,
		ShptExportDelegateFactory factory) throws Exception {
		// support query by path (i.e. all files in these paths)
		// support query by Sharepoint query language
		if (service == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		final String path = configuration.getString(ShptSetting.PATH);
		if (path == null) { throw new ShptException("Must provide the name of the site to export"); }
		final boolean excludeEmptyFolders = configuration.getBoolean(ShptSetting.EXCLUDE_EMPTY_FOLDERS);

		try {
			return new CloseableIteratorWrapper<ExportTarget>(
				new ShptRecursiveIterator(service, service.getFolder(path), configuration, excludeEmptyFolders));
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
	protected ShptExportContextFactory newContextFactory(ShptSession session, CfgTools cfg) throws Exception {
		return new ShptExportContextFactory(this, cfg, session);
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