package com.armedia.caliente.engine.documentum.exporter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.engine.documentum.DctmObjectType;
import com.armedia.caliente.engine.documentum.DctmSessionWrapper;
import com.armedia.caliente.engine.documentum.UnsupportedDctmObjectTypeException;
import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.IDfValue;

public class DctmExportDelegateFactory
	extends ExportDelegateFactory<IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportEngine> {

	final Map<String, Set<String>> pathCache = Collections.synchronizedMap(new HashMap<String, Set<String>>());
	final Map<String, Set<String>> pathIdCache = Collections.synchronizedMap(new HashMap<String, Set<String>>());

	DctmExportDelegateFactory(DctmExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	public void close() {
		this.pathCache.clear();
		this.pathIdCache.clear();
		super.close();
	}

	@Override
	protected DctmExportDelegate<?> newExportDelegate(IDfSession session, CmfType type, String searchKey)
		throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the object"); }
		if (searchKey == null) { throw new IllegalArgumentException("Must provide an object ID to retrieve"); }
		return newExportDelegate(session.getObject(new DfId(searchKey)), type);
	}

	DctmExportDelegate<?> newExportDelegate(IDfPersistentObject object) throws Exception {
		return newExportDelegate(object, null);
	}

	DctmExportDelegate<?> newExportDelegate(IDfPersistentObject object, CmfType type) throws Exception {
		if ("08de75d18002b162,08de75d18002b163".toLowerCase()
			.indexOf(object.getObjectId().getId().toLowerCase()) >= 0) {
			"".hashCode();
		}
		// For Documentum, the type is not used for the search. We do, however, use it to validate
		// the returned object...
		String typeStr = null;
		DctmObjectType dctmType = null;
		if (type != null) {
			typeStr = type.name();
			dctmType = DctmObjectType.decodeType(type);
		} else {
			typeStr = object.getType().getName();
			try {
				dctmType = DctmObjectType.decodeType(object);
			} catch (UnsupportedDctmObjectTypeException e) {
				dctmType = null;
			}
		}
		if (dctmType == null) { throw new ExportException(
			String.format("Unsupported object type [%s] (objectId = [%s])", typeStr, object.getObjectId().getId())); }

		Class<? extends IDfPersistentObject> requiredClass = dctmType.getDfClass();
		if (requiredClass.isInstance(object)) {
			DctmExportDelegate<?> delegate = null;
			switch (dctmType) {
				case STORE:
					delegate = new DctmExportStore(this, object);
					break;
				case USER:
					delegate = new DctmExportUser(this, object);
					break;
				case GROUP:
					delegate = new DctmExportGroup(this, object);
					break;
				case ACL:
					delegate = new DctmExportACL(this, object);
					break;
				case TYPE:
					delegate = new DctmExportType(this, object);
					break;
				case FORMAT:
					delegate = new DctmExportFormat(this, object);
					break;
				case FOLDER:
					delegate = new DctmExportFolder(this, object);
					break;
				case DOCUMENT:
					delegate = new DctmExportDocument(this, object);
					break;
				default:
					break;
			}
			return delegate;
		}
		this.log.warn(String.format("Type [%s] is not supported - no delegate created for search key [%s]", type,
			object.getObjectId().getId()));
		return null;
	}
}