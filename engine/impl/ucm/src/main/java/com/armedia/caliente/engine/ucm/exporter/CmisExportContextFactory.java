package com.armedia.caliente.engine.ucm.exporter;

import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.engine.ucm.CmisSessionWrapper;
import com.armedia.caliente.engine.ucm.PermissionMapper;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisExportContextFactory
	extends ExportContextFactory<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportEngine> {

	private final RepositoryInfo repositoryInfo;
	private final PermissionMapper permissionMapper;

	CmisExportContextFactory(CmisExportEngine engine, Session session, CfgTools settings,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Logger output,
		WarningTracker warningTracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
		this.repositoryInfo = session.getRepositoryInfo();
		if (super.isSupported(CmfType.ACL)) {
			this.permissionMapper = new PermissionMapper(session);
		} else {
			this.permissionMapper = null;
		}
	}

	public final RepositoryInfo getRepositoryInfo() {
		return this.repositoryInfo;
	}

	public Set<String> convertPermissionToAllowableActions(String permission) {
		if (this.permissionMapper == null) { return null; }
		return this.permissionMapper.convertPermissionToAllowableActions(permission);
	}

	@Override
	protected CmisExportContext constructContext(String rootId, CmfType rootType, Session session, int batchPosition) {
		return new CmisExportContext(this, rootId, rootType, session, getOutput(), getWarningTracker());
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