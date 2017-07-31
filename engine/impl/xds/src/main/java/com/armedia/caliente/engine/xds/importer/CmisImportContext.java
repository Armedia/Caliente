package com.armedia.caliente.engine.xds.importer;

import java.util.Collection;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.caliente.store.CmfValue;

public class CmisImportContext extends ImportContext<Session, CmfValue, CmisImportContextFactory> {

	CmisImportContext(CmisImportContextFactory factory, String rootId, CmfType rootType, Session session, Logger output,
		WarningTracker warningTracker, CmfTypeMapper typeMapper, CmfAttributeTranslator<CmfValue> translator,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, int batchPosition) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, warningTracker, typeMapper, translator,
			objectStore, streamStore, batchPosition);
	}

	public Set<String> convertAllowableActionsToPermissions(Collection<String> allowableActions) {
		return getFactory().convertAllowableActionsToPermissions(allowableActions);
	}

	public final RepositoryInfo getRepositoryInfo() {
		return getFactory().getRepositoryInfo();
	}
}