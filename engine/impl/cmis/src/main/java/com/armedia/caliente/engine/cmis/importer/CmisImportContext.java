package com.armedia.caliente.engine.cmis.importer;

import java.util.Collection;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;

public class CmisImportContext extends ImportContext<Session, CmfValue, CmisImportContextFactory> {

	CmisImportContext(CmisImportContextFactory factory, String rootId, CmfObject.Archetype rootType, Session session,
		Logger output, WarningTracker warningTracker, Transformer transformer,
		CmfAttributeTranslator<CmfValue> translator, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> streamStore, int batchPosition) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, warningTracker, transformer,
			translator, objectStore, streamStore, batchPosition);
	}

	public Set<String> convertAllowableActionsToPermissions(Collection<String> allowableActions) {
		return getFactory().convertAllowableActionsToPermissions(allowableActions);
	}

	public final RepositoryInfo getRepositoryInfo() {
		return getFactory().getRepositoryInfo();
	}
}