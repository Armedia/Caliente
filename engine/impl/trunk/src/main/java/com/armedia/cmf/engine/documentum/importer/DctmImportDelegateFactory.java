package com.armedia.cmf.engine.documentum.importer;

import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmTranslator;
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;
import com.documentum.fc.common.IDfValue;

public class DctmImportDelegateFactory {

	public static DctmImportDelegate<?> newDelegate(DctmImportEngine engine, StoredObject<IDfValue> marshaled)
		throws UnsupportedDctmObjectTypeException, UnsupportedObjectTypeException {
		DctmObjectType type = DctmTranslator.translateType(marshaled.getType());
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