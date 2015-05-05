package com.armedia.cmf.engine.documentum.importer;

import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public class DctmImportDelegateFactory extends
ImportDelegateFactory<IDfSession, DctmSessionWrapper, IDfValue, DctmImportContext, DctmImportEngine> {

	protected DctmImportDelegateFactory(DctmImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	public static DctmImportDelegate<?> newDelegate(DctmImportEngine engine, StoredObject<IDfValue> marshaled)
		throws UnsupportedObjectTypeException, UnsupportedDctmObjectTypeException {
		DctmObjectType type = DctmObjectType.decodeType(marshaled.getType());
		if (type == null) { throw new UnsupportedObjectTypeException(marshaled.getType()); }
		switch (type) {
			case ACL:
				return new DctmImportACL(engine, marshaled);
			case DOCUMENT:
				return new DctmImportDocument(engine, marshaled);
			case STORE:
				return new DctmImportStore(engine, marshaled);
			case FOLDER:
				return new DctmImportFolder(engine, marshaled);
			case FORMAT:
				return new DctmImportFormat(engine, marshaled);
			case GROUP:
				return new DctmImportGroup(engine, marshaled);
			case TYPE:
				return new DctmImportType(engine, marshaled);
			case USER:
				return new DctmImportUser(engine, marshaled);
			default:
				break;
		}
		throw new UnsupportedDctmObjectTypeException(String.format("Type [%s] is not supported", type.name()));
	}
}