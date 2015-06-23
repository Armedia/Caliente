package com.armedia.cmf.engine.cmis.importer;

import java.util.Collection;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.cmf.storage.CmfValue;

public class CmisImportContext extends ImportContext<Session, CmfValue, CmisImportContextFactory> {

	CmisImportContext(CmisImportContextFactory factory, String rootId, CmfType rootType, Session session,
		Logger output, CmfTypeMapper typeMapper, CmfAttributeTranslator<CmfValue> translator,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, typeMapper, translator, objectStore,
			streamStore);
	}

	public Set<String> convertAllowableActionsToPermissions(Collection<String> allowableActions) {
		return getFactory().convertAllowableActionsToPermissions(allowableActions);
	}

	public final RepositoryInfo getRepositoryInfo() {
		return getFactory().getRepositoryInfo();
	}
}