package com.armedia.caliente.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;

public class CmisUserDelegate extends CmisExportDelegate<FileableCmisObject> {

	protected CmisUserDelegate(CmisExportDelegateFactory factory, Session session, FileableCmisObject object)
		throws Exception {
		super(factory, session, FileableCmisObject.class, object);
	}

	@Override
	protected CmfType calculateType(Session session, FileableCmisObject object) throws Exception {
		return CmfType.USER;
	}

	@Override
	protected String calculateLabel(Session session, FileableCmisObject object) throws Exception {
		return object.getCreatedBy();
	}

	@Override
	protected String calculateObjectId(Session session, FileableCmisObject object) throws Exception {
		return String.format("%s", object.getCreatedBy());
	}

	@Override
	protected String calculateSearchKey(Session session, FileableCmisObject object) throws Exception {
		return object.getCreatedBy();
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		CmfAttribute<CmfValue> userName = new CmfAttribute<>(IntermediateAttribute.NAME, CmfDataType.STRING, false);
		userName.setValue(new CmfValue(this.object.getCreatedBy()));
		userName = new CmfAttribute<>(IntermediateAttribute.LOGIN_NAME, CmfDataType.STRING, false);
		userName.setValue(new CmfValue(this.object.getCreatedBy()));
		return true;
	}

	@Override
	protected String calculateName(Session session, FileableCmisObject object) throws Exception {
		return object.getCreatedBy();
	}
}