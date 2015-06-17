package com.armedia.cmf.engine.cmis.importer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.slf4j.Logger;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.importer.ImportContextFactory;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisImportContextFactory extends
	ImportContextFactory<Session, CmisSessionWrapper, CmfValue, CmisImportContext, CmisImportEngine, Folder> {

	private final Map<String, Set<String>> actionsToPermissions;
	private final RepositoryInfo repositoryInfo;

	CmisImportContextFactory(CmisImportEngine engine, Session session, CfgTools settings) {
		super(engine, settings);
		this.repositoryInfo = session.getRepositoryInfo();

		Map<String, Set<String>> actionsToPermissions = new TreeMap<String, Set<String>>();

		Set<String> actions = new HashSet<String>();
		for (Map.Entry<String, PermissionMapping> mapping : session.getRepositoryInfo().getAclCapabilities()
			.getPermissionMapping().entrySet()) {
			Set<String> permissions = actionsToPermissions.get(mapping.getKey());
			if (permissions == null) {
				permissions = new TreeSet<String>();
				actionsToPermissions.put(mapping.getKey(), permissions);
				actions.add(mapping.getKey());
			}
			for (String permission : mapping.getValue().getPermissions()) {
				permissions.add(permission);
			}
		}

		// Use Linked* to preserve order, but get better performance
		for (String a : actions) {
			Set<String> s = actionsToPermissions.get(a);
			actionsToPermissions.put(a, Tools.freezeSet(new LinkedHashSet<String>(s)));
		}
		this.actionsToPermissions = Tools.freezeMap(new LinkedHashMap<String, Set<String>>(actionsToPermissions));
	}

	public final RepositoryInfo getRepositoryInfo() {
		return this.repositoryInfo;
	}

	public Set<String> convertAllowableActionsToPermissions(Collection<String> allowableActions) {
		Set<String> ret = new TreeSet<String>();
		for (String action : allowableActions) {
			Set<String> p = this.actionsToPermissions.get(action);
			if ((p == null) || p.isEmpty()) {
				continue;
			}
			ret.addAll(p);
		}
		return ret;
	}

	@Override
	protected CmisImportContext constructContext(String rootId, CmfType rootType, Session session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore) {
		return new CmisImportContext(this, rootId, rootType, session, output, getEngine().getTranslator(), objectStore,
			streamStore);
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
}