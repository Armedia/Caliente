package com.armedia.cmf.engine.cmis.exporter;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.slf4j.Logger;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.cmis.PermissionMapper;
import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisExportContextFactory extends
	ExportContextFactory<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportEngine> {

	private final RepositoryInfo repositoryInfo;
	private final PermissionMapper permissionMapper;

	CmisExportContextFactory(CmisExportEngine engine, Session session, CfgTools settings) throws Exception {
		super(engine, settings, session);
		this.repositoryInfo = session.getRepositoryInfo();
		this.permissionMapper = new PermissionMapper(session);
	}

	public final RepositoryInfo getRepositoryInfo() {
		return this.repositoryInfo;
	}

	public Set<String> convertPermissionToAllowableActions(String permission) {
		return this.permissionMapper.convertPermissionToAllowableActions(permission);
	}

	@Override
	protected CmisExportContext constructContext(String rootId, CmfType rootType, Session session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore, CmfTypeMapper typeMapper) {
		return new CmisExportContext(this, rootId, rootType, session, output);
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