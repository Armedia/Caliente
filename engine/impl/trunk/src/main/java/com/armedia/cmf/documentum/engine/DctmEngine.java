package com.armedia.cmf.documentum.engine;

import com.armedia.cmf.engine.Engine;
import com.armedia.cmf.engine.SessionFactory;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public abstract class DctmEngine implements Engine<IDfSession, IDfPersistentObject, IDfValue> {

	@Override
	public void init(CfgTools config) throws Exception {
	}

	@Override
	public ObjectStorageTranslator<IDfPersistentObject, IDfValue> getTranslator() {
		return DctmTranslator.INSTANCE;
	}

	@Override
	public SessionFactory<IDfSession> getSessionFactory() {
		return new DctmSessionFactory();
	}

	@Override
	public void close() {
	}
}