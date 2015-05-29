package com.armedia.cmf.engine.cmis.exporter;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.Tools;

public class CmisExportContext extends ExportContext<Session, CmfValue> {

	private final Map<String, Set<String>> permToActions;

	CmisExportContext(CmisExportContextFactory factory, String rootId, CmfType rootType, Session session, Logger output) {
		super(factory, factory.getSettings(), rootId, rootType, session, output);
		Map<String, Set<String>> permToActions = new TreeMap<String, Set<String>>();
		Map<String, PermissionMapping> m = session.getRepositoryInfo().getAclCapabilities().getPermissionMapping();
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
		}
		return ret;
	}

	public OperationContext newOperationContext() {
		OperationContext parent = getSession().getDefaultContext();
		OperationContext ctx = getSession().createOperationContext();
		ctx.setCacheEnabled(parent.isCacheEnabled());
		ctx.setFilter(parent.getFilter());
		ctx.setFilterString(parent.getFilterString());
		ctx.setIncludeAcls(parent.isIncludeAcls());
		ctx.setIncludeAllowableActions(parent.isIncludeAllowableActions());
		ctx.setIncludePathSegments(parent.isIncludePathSegments());
		ctx.setIncludePolicies(parent.isIncludePolicies());
		ctx.setIncludeRelationships(parent.getIncludeRelationships());
		ctx.setLoadSecondaryTypeProperties(parent.loadSecondaryTypeProperties());
		ctx.setMaxItemsPerPage(parent.getMaxItemsPerPage());
		ctx.setOrderBy(parent.getOrderBy());
		ctx.setRenditionFilter(parent.getRenditionFilter());
		ctx.setRenditionFilterString(parent.getRenditionFilterString());
		return ctx;
	}
}