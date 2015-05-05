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
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

public class CmisAclDelegate extends CmisExportDelegate<CmisAcl> {

	public CmisAclDelegate(CmisExportEngine engine, CmisObject object, CfgTools configuration) throws Exception {
		super(engine, CmisAcl.class, new CmisAcl(engine.decodeType(object.getType()), object), configuration);
	}

	@Override
	protected Collection<CmisExportDelegate<?>> identifyRequirements(StoredObject<StoredValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return super.identifyRequirements(marshalled, ctx);
	}

	@Override
	protected void marshal(CmisExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		StoredAttribute<StoredValue> att = new StoredAttribute<StoredValue>(CmisCustomAttributes.ACL_OWNER.name,
			StoredDataType.STRING, false);
		att.setValue(new StoredValue(this.object.getSourceOwner()));
		object.setAttribute(att);
		att = new StoredAttribute<StoredValue>(PropertyIds.NAME, StoredDataType.STRING, false);
		att.setValue(new StoredValue(this.object.getSourceId()));
		object.setAttribute(att);

	}

	@Override
	protected final Collection<CmisExportDelegate<?>> identifyDependents(StoredObject<StoredValue> marshalled,
		CmisExportContext ctx) throws Exception {
		return super.identifyDependents(marshalled, ctx);
	}

	@Override
	protected final List<ContentInfo> storeContent(Session session, StoredObject<StoredValue> marshalled,
		ExportTarget referrent, ContentStore streamStore) throws Exception {
		return super.storeContent(session, marshalled, referrent, streamStore);
	}

	@Override
	protected final StoredObjectType calculateType(CmisAcl object) throws Exception {
		return StoredObjectType.ACL;
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