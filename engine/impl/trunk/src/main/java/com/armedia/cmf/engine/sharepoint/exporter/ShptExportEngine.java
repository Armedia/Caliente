/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
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
import com.armedia.cmf.engine.sharepoint.types.ShptFile;
import com.armedia.cmf.engine.sharepoint.types.ShptFolder;
import com.armedia.cmf.engine.sharepoint.types.ShptGroup;
import com.armedia.cmf.engine.sharepoint.types.ShptObject;
import com.armedia.cmf.engine.sharepoint.types.ShptUser;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public class ShptExportEngine extends ExportEngine<ShptSession, ShptSessionWrapper, StoredValue, ShptExportContext> {

	private static final Set<String> TARGETS = Collections.singleton(ShptObject.TARGET_NAME);

	@Override
	protected Iterator<ExportTarget> findExportResults(ShptSession service, Map<String, ?> settings) throws Exception {
		// support query by path (i.e. all files in these paths)
		// support query by Sharepoint query language
		if (service == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		if (settings == null) {
			settings = Collections.emptyMap();
		}
		final String path = CfgTools.decodeString(Setting.PATH, settings);
		if (path == null) { throw new ShptException("Must provide the name of the site to export"); }
		final boolean excludeEmptyFolders = CfgTools.decodeBoolean(Setting.EXCLUDE_EMPTY_FOLDERS, settings);

		try {
			return new ShptRecursiveIterator(this, service, service.getFolder(path), excludeEmptyFolders);
		} catch (ShptSessionException e) {
			throw new ShptException("Export target search failed", e);
		}
	}

	@Override
	protected ShptExportDelegate<?> getExportDelegate(ShptSession session, StoredObjectType type, String searchKey)
		throws Exception {
		switch (type) {
			case USER:
				return new ShptUser(this, session.getUser(Tools.decodeInteger(searchKey)));
			case GROUP:
				return new ShptGroup(this, session.getGroup(Tools.decodeInteger(searchKey)));
			case FOLDER:
				return new ShptFolder(this, session.getFolder(searchKey));
			case DOCUMENT:
				return ShptFile.locateFile(this, session, searchKey);
			default:
				throw new Exception(String.format("Unsupported object type [%s]", type));
		}
	}

	@Override
	protected StoredValue getValue(StoredDataType type, Object value) {
		try {
			return new StoredValue(type, value);
		} catch (ParseException e) {
			throw new RuntimeException("Exception raised while creating a new value", e);
		}
	}

	@Override
	protected ObjectStorageTranslator<StoredValue> getTranslator() {
		return ShptTranslator.INSTANCE;
	}

	@Override
	protected ShptSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new ShptSessionFactory(cfg);
	}

	@Override
	protected ShptExportContextFactory newContextFactory(CfgTools cfg) throws Exception {
		return new ShptExportContextFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return ShptExportEngine.TARGETS;
	}

	public static ExportEngine<?, ?, ?, ?> getExportEngine() {
		return TransferEngine.getTransferEngine(ExportEngine.class, ShptExportEngine.TARGETS.iterator().next());
	}
}