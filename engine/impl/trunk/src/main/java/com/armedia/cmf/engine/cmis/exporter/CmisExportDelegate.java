package com.armedia.cmf.engine.cmis.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public abstract class CmisExportDelegate<T>
	extends
	ExportDelegate<T, Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportDelegateFactory, CmisExportEngine> {

	protected CmisExportDelegate(CmisExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return new ArrayList<CmisExportDelegate<?>>();
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshalled, CmisExportContext ctx)
		throws Exception {
		return new ArrayList<CmisExportDelegate<?>>();
	}

	@Override
	protected List<CmfContentInfo> storeContent(Session session, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?> streamStore) throws Exception {
		return new ArrayList<CmfContentInfo>();
	}
}