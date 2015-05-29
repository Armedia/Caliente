package com.armedia.cmf.engine.cmis.exporter;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.slf4j.Logger;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportContextFactory extends
	ExportContextFactory<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportEngine> {

	private final Map<String, Set<String>> permToActions;

	CmisExportContextFactory(CmisExportEngine engine, Session session, CfgTools settings) {
		super(engine, settings);
		Map<String, Set<String>> permToActions = new TreeMap<String, Set<String>>();
		Map<String, PermissionMapping> m = session.getRepositoryInfo().getAclCapabilities().getPermissionMapping();

		/*
		Map<String, String> allPerms = new TreeMap<String, String>();
		for (PermissionDefinition pd : session.getRepositoryInfo().getAclCapabilities().getPermissions()) {
			allPerms.put(pd.getId(), pd.getDescription());
		}
		 */
		Set<String> permissions = new HashSet<String>();
		for (String action : m.keySet()) {
			PermissionMapping mapping = m.get(action);
			for (String permission : mapping.getPermissions()) {
				permissions.add(permission);
				Set<String> s = permToActions.get(permission);
				if (s == null) {
					s = new TreeSet<String>();
					permToActions.put(permission, s);
				}
				s.add(action);
			}
		}

		// Use Linked* to preserve order, but get better performance
		for (String p : permissions) {
			Set<String> s = permToActions.get(p);
			permToActions.put(p, Tools.freezeSet(new LinkedHashSet<String>(s)));
		}
		this.permToActions = Tools.freezeMap(new LinkedHashMap<String, Set<String>>(permToActions));
	}

	public Set<String> convertPermissionToAllowableActions(String permission) {
		Set<String> ret = this.permToActions.get(permission);
		if (ret == null) {
			ret = Collections.emptySet();
		} else {
			ret.size();
		}
		return ret;
	}

	@Override
	protected CmisExportContext constructContext(String rootId, CmfType rootType, Session session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore) {
		return new CmisExportContext(this, rootId, rootType, session, output);
	}
}