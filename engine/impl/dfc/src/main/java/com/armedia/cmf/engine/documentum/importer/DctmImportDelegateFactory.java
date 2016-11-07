package com.armedia.cmf.engine.documentum.importer;

import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.cmf.engine.importer.ImportDelegateFactory;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.UnsupportedCmfTypeException;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

public class DctmImportDelegateFactory extends
	ImportDelegateFactory<IDfSession, DctmSessionWrapper, IDfValue, DctmImportContext, DctmImportEngine> {

	protected DctmImportDelegateFactory(DctmImportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected DctmImportDelegate<?> newImportDelegate(CmfObject<IDfValue> marshaled) throws Exception {
		DctmObjectType type = DctmObjectType.decodeType(marshaled.getType());
		if (type == null) { throw new UnsupportedCmfTypeException(marshaled.getType()); }
		switch (type) {
			case DOCUMENT:
				return new DctmImportDocument(this, marshaled);
			case STORE:
				return new DctmImportStore(this, marshaled);
			case FOLDER:
				return new DctmImportFolder(this, marshaled);
			case FORMAT:
				return new DctmImportFormat(this, marshaled);
			case GROUP:
				return new DctmImportGroup(this, marshaled);
			case ACL:
				return new DctmImportACL(this, marshaled);
			case TYPE:
				return new DctmImportType(this, marshaled);
			case USER:
				return new DctmImportUser(this, marshaled);
			default:
				break;
		}
		throw new UnsupportedDctmObjectTypeException(String.format("Type [%s] is not supported", type.name()));
	}
}