/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.documentum.common.DctmSpecialValues;
import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportContextFactory
	extends
	ImportContextFactory<IDfSession, DctmSessionWrapper, IDfPersistentObject, IDfValue, DctmImportContext, DctmImportEngine> {
	private final DctmSpecialValues specialValues;

	DctmImportContextFactory(DctmImportEngine engine, CfgTools cfg) {
		super(engine, cfg);
		this.specialValues = new DctmSpecialValues(cfg);
	}

	@Override
	protected DctmImportContext constructContext(String rootId, StoredObjectType rootType, IDfSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		return new DctmImportContext(this, rootId, rootType, session, output, objectStore, contentStore);
	}

	public final DctmSpecialValues getSpecialValues() {
		return this.specialValues;
	}

	@Override
	protected IDfPersistentObject locateOrCreatePath(IDfSession session, String path) throws Exception {
		return doEnsurePath(session, path);
	}

	public final IDfFolder ensurePath(IDfSession session, String path) throws Exception {
		if (session == null) { throw new IllegalArgumentException("Must provide a session to work with"); }
		if (path == null) { throw new IllegalArgumentException("Must provide a path to ensure"); }
		if (!path.startsWith("/")) { throw new IllegalArgumentException(String.format("The path [%s] is not absolute",
			path)); }
		return doEnsurePath(session, FileNameTools.normalizePath(path, '/'));
	}

	private IDfFolder doEnsurePath(IDfSession session, String path) throws Exception {
		IDfFolder f = session.getFolderByPath(path);
		if (f != null) { return f; }

		final String dirName = FileNameTools.dirname(path, '/');
		final String baseName = FileNameTools.basename(path, '/');
		final boolean cabinet = Tools.equals("/", dirName);
		final String type = (cabinet ? "dm_cabinet" : DctmObjectType.FOLDER.getDmType());
		f = IDfFolder.class.cast(session.newObject(type));
		f.setObjectName(baseName);
		if (!cabinet) {
			final IDfFolder parent = doEnsurePath(session, dirName);
			f.link(parent.getObjectId().getId());
			parent.save();
		}
		f.save();
		return f;
	}
}