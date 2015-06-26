/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.documentum.common.DctmSpecialValues;
import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportContextFactory extends
	ImportContextFactory<IDfSession, DctmSessionWrapper, IDfValue, DctmImportContext, DctmImportEngine, IDfFolder> {
	private final DctmSpecialValues specialValues;

	DctmImportContextFactory(DctmImportEngine engine, CfgTools cfg, IDfSession session) throws Exception {
		super(engine, cfg, session);
		this.specialValues = new DctmSpecialValues(cfg);
	}

	@Override
	protected DctmImportContext constructContext(String rootId, CmfType rootType, IDfSession session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> contentStore, CmfTypeMapper typeMapper) {
		return new DctmImportContext(this, getSettings(), rootId, rootType, session, output, typeMapper, getEngine()
			.getTranslator(), objectStore, contentStore);
	}

	public final DctmSpecialValues getSpecialValues() {
		return this.specialValues;
	}

	@Override
	protected IDfFolder locateFolder(IDfSession session, String path) throws Exception {
		return session.getFolderByPath(path);
	}

	@Override
	protected IDfFolder createFolder(IDfSession session, IDfFolder parent, String name) throws Exception {
		final String type = (parent != null ? DctmObjectType.FOLDER.getDmType() : "dm_cabinet");
		IDfFolder f = IDfFolder.class.cast(session.newObject(type));
		f.setObjectName(name);
		if (parent != null) {
			f.link(parent.getObjectId().getId());
		}
		f.save();
		return f;
	}

	@Override
	protected String calculateProductName(IDfSession session) {
		return "Documentum";
	}

	@Override
	protected String calculateProductVersion(IDfSession session) throws Exception {
		return session.getServerVersion();
	}
}