package com.armedia.cmf.engine.cmis.exporter;

import java.util.Collection;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.cmis.CmisAcl;
import com.armedia.cmf.engine.cmis.CmisCustomAttributes;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;

public class CmisAclDelegate extends CmisExportDelegate<CmisAcl> {

	public CmisAclDelegate(CmisExportDelegateFactory factory, CmisObject object) throws Exception {
		super(factory, CmisAcl.class, new CmisAcl(factory.getEngine().decodeType(object.getType()), object));
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return super.identifyRequirements(marshalled, ctx);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		CmfAttribute<CmfValue> att = new CmfAttribute<CmfValue>(CmisCustomAttributes.ACL_OWNER.name,
			CmfDataType.STRING, false);
		att.setValue(new CmfValue(this.object.getSourceOwner()));
		object.setAttribute(att);
		att = new CmfAttribute<CmfValue>(PropertyIds.NAME, CmfDataType.STRING, false);
		att.setValue(new CmfValue(this.object.getSourceId()));
		object.setAttribute(att);
		return true;
	}

	@Override
	protected final Collection<CmisExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return super.identifyDependents(marshalled, ctx);
	}

	@Override
	protected final List<ContentInfo> storeContent(Session session, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?> streamStore) throws Exception {
		return super.storeContent(session, translator, marshalled, referrent, streamStore);
	}

	@Override
	protected final CmfType calculateType(CmisAcl object) throws Exception {
		return CmfType.ACL;
	}

	@Override
	protected String calculateLabel(CmisAcl object) throws Exception {
		return String.format("ACL::[%s:%s]", object.getSourceType(), object.getSourceId());
	}

	@Override
	protected String calculateObjectId(CmisAcl object) throws Exception {
		return object.getSourceId();
	}

	@Override
	protected String calculateSearchKey(CmisAcl object) throws Exception {
		return object.getSourceId();
	}
}