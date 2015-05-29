package com.armedia.cmf.engine.cmis.exporter;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;

import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;

public class CmisAclDelegate extends CmisExportDelegate<FileableCmisObject> {

	protected CmisAclDelegate(CmisExportDelegateFactory factory, FileableCmisObject object) throws Exception {
		super(factory, FileableCmisObject.class, object);
	}

	@Override
	protected CmfType calculateType(FileableCmisObject object) throws Exception {
		return CmfType.ACL;
	}

	@Override
	protected String calculateLabel(FileableCmisObject object) throws Exception {
		final String p;
		List<String> paths = object.getPaths();
		if ((paths != null) && !paths.isEmpty()) {
			p = String.format("%s/%s", paths.get(0), object.getName());
		} else {
			p = String.format("unfiled://%s", object.getName());
		}
		return String.format("ACL-[%s]-[%s]", object.getType().getId(), p);
	}

	@Override
	protected String calculateObjectId(FileableCmisObject object) throws Exception {
		return String.format("ACL-[%s]", object.getId());
	}

	@Override
	protected String calculateSearchKey(FileableCmisObject object) throws Exception {
		return object.getId();
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		// Copy the ACL Data into the object's attributes using the common ACL attributes
		Acl acl = this.object.getAcl();
		for (Ace ace : acl.getAces()) {
			Set<String> actions = new TreeSet<String>();
			for (String permission : ace.getPermissions()) {
				actions.addAll(ctx.convertPermissionToAllowableActions(permission));
			}
			actions.size();
		}
		return true;
	}
}