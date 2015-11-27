package com.armedia.cmf.engine.cmis.importer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.slf4j.Logger;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.cmis.PermissionMapper;
import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisImportContextFactory extends
	ImportContextFactory<Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportEngine, Folder> {

	private final PermissionMapper permissionMapper;
	private final RepositoryInfo repositoryInfo;

	CmisImportContextFactory(CmisImportEngine engine, Session session, CfgTools settings) throws Exception {
		super(engine, settings, session);
		this.repositoryInfo = session.getRepositoryInfo();
		this.permissionMapper = new PermissionMapper(session);
	}

	public final RepositoryInfo getRepositoryInfo() {
		return this.repositoryInfo;
	}

	public Set<String> convertAllowableActionsToPermissions(Collection<String> allowableActions) {
		return this.permissionMapper.convertAllowableActionsToPermissions(allowableActions);
	}

	@Override
	protected CmisImportContext constructContext(String rootId, CmfType rootType, Session session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, CmfTypeMapper typeMapper) {
		return new CmisImportContext(this, rootId, rootType, session, output, typeMapper, getEngine().getTranslator(),
			objectStore, streamStore);
	}

	private boolean isFolderType(ObjectType type) {
		if (type == null) { throw new IllegalArgumentException("Must provide an object type"); }
		if (!type.isBaseType()) {
			type = type.getBaseType();
		}
		return (type.getBaseTypeId() == BaseTypeId.CMIS_FOLDER);
	}

	@Override
	protected Folder locateFolder(Session session, String path) throws Exception {
		try {
			CmisObject obj = session.getObjectByPath(path);
			if (obj == null) { return null; }
			if (isFolderType(obj.getType()) || (obj instanceof Folder)) { return Folder.class.cast(obj); }
		} catch (CmisObjectNotFoundException e) {
			// Do nothing...
		}
		return null;
	}

	@Override
	protected Folder createFolder(Session session, Folder parent, String name) throws Exception {
		if (parent == null) {
			parent = session.getRootFolder();
		}
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
		properties.put(PropertyIds.NAME, name);
		return parent.createFolder(properties);
	}

	@Override
	public final String calculateProductName(Session session) {
		return session.getRepositoryInfo().getProductName();
	}

	@Override
	public final String calculateProductVersion(Session session) {
		return session.getRepositoryInfo().getProductVersion();
	}
}