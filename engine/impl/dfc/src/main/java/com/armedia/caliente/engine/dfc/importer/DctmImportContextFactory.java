/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.common.DctmSpecialValues;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContextFactory;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
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

	DctmImportContextFactory(DctmImportEngine engine, CfgTools cfg, IDfSession session,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		super(engine, cfg, session, objectStore, contentStore, transformer, output, warningTracker);
		this.specialValues = new DctmSpecialValues(cfg);
	}

	@Override
	protected DctmImportContext constructContext(String rootId, CmfObject.Archetype rootType, IDfSession session,
		int historyPosition) {
		return new DctmImportContext(this, getSettings(), rootId, rootType, session, getOutput(), getWarningTracker(),
			getTransformer(), getEngine().getTranslator(), getObjectStore(), getContentStore(), historyPosition);
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