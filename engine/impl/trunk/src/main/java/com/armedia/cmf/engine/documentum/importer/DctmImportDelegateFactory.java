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

	@Override
	protected DctmImportDelegate<?> newImportDelegate(StoredObject<?> marshaled) throws Exception {
		DctmObjectType type = DctmObjectType.decodeType(marshaled.getType());
		if (type == null) { throw new UnsupportedObjectTypeException(marshaled.getType()); }
		@SuppressWarnings("unchecked")
		final StoredObject<IDfValue> castedMarshaled = (StoredObject<IDfValue>) marshaled;
		switch (type) {
			case ACL:
				return new DctmImportACL(this, castedMarshaled);
			case DOCUMENT:
				return new DctmImportDocument(this, castedMarshaled);
			case STORE:
				return new DctmImportStore(this, castedMarshaled);
			case FOLDER:
				return new DctmImportFolder(this, castedMarshaled);
			case FORMAT:
				return new DctmImportFormat(this, castedMarshaled);
			case GROUP:
				return new DctmImportGroup(this, castedMarshaled);
			case TYPE:
				return new DctmImportType(this, castedMarshaled);
			case USER:
				return new DctmImportUser(this, castedMarshaled);
			default:
				break;
		}
		throw new UnsupportedDctmObjectTypeException(String.format("Type [%s] is not supported", type.name()));
	}
}