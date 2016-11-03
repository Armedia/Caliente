package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;

public class CmisUserDelegate extends CmisExportDelegate<FileableCmisObject> {

	protected CmisUserDelegate(CmisExportDelegateFactory factory, FileableCmisObject object) throws Exception {
		super(factory, FileableCmisObject.class, object);
	}

	@Override
	protected CmfType calculateType(FileableCmisObject object) throws Exception {
		return CmfType.USER;
	}

	@Override
	protected String calculateLabel(FileableCmisObject object) throws Exception {
		return object.getCreatedBy();
	}

	@Override
	protected String calculateObjectId(FileableCmisObject object) throws Exception {
		return String.format("%s", object.getCreatedBy());
	}

	@Override
	protected String calculateSearchKey(FileableCmisObject object) throws Exception {
		return object.getCreatedBy();
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		CmfAttribute<CmfValue> userName = new CmfAttribute<CmfValue>(IntermediateAttribute.NAME, CmfDataType.STRING,
			false);
		userName.setValue(new CmfValue(this.object.getCreatedBy()));
		userName = new CmfAttribute<CmfValue>(IntermediateAttribute.LOGIN_NAME, CmfDataType.STRING, false);
		userName.setValue(new CmfValue(this.object.getCreatedBy()));
		return true;
	}

	@Override
	protected String calculateName(FileableCmisObject object) throws Exception {
		return object.getCreatedBy();
	}
}