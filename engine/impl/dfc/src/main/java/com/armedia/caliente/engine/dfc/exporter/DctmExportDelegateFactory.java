package com.armedia.caliente.engine.dfc.exporter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.UnsupportedDctmObjectTypeException;
import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfObject;
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
	protected DctmExportDelegate<?> newExportDelegate(IDfSession session, CmfObject.Archetype type, String searchKey)
		throws Exception {
		if (session == null) {
			throw new IllegalArgumentException("Must provide a session through which to retrieve the object");
		}
		if (searchKey == null) { throw new IllegalArgumentException("Must provide an object ID to retrieve"); }
		return newExportDelegate(session, session.getObject(new DfId(searchKey)), type);
	}

	DctmExportDelegate<?> newExportDelegate(IDfSession session, IDfPersistentObject object) throws Exception {
		return newExportDelegate(session, object, null);
	}

	DctmExportDelegate<?> newExportDelegate(IDfSession session, IDfPersistentObject object, CmfObject.Archetype type)
		throws Exception {
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
		if (dctmType == null) {
			throw new ExportException(
				String.format("Unsupported object type [%s] (objectId = [%s])", typeStr, object.getObjectId().getId()));
		}

		Class<? extends IDfPersistentObject> requiredClass = dctmType.getDfClass();
		if (requiredClass.isInstance(object)) {
			DctmExportDelegate<?> delegate = null;
			switch (dctmType) {
				case STORE:
					delegate = new DctmExportStore(this, session, object);
					break;
				case USER:
					delegate = new DctmExportUser(this, session, object);
					break;
				case GROUP:
					delegate = new DctmExportGroup(this, session, object);
					break;
				case ACL:
					delegate = new DctmExportACL(this, session, object);
					break;
				case TYPE:
					delegate = new DctmExportType(this, session, object);
					break;
				case FORMAT:
					delegate = new DctmExportFormat(this, session, object);
					break;
				case FOLDER:
					delegate = new DctmExportFolder(this, session, object);
					break;
				case DOCUMENT:
					delegate = new DctmExportDocument(this, session, object);
					break;
				default:
					break;
			}
			return delegate;
		}
		this.log.warn("Type [{}] is not supported - no delegate created for search key [{}]", type,
			object.getObjectId().getId());
		return null;
	}
}