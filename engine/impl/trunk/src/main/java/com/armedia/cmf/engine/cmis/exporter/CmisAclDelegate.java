package com.armedia.cmf.engine.cmis.exporter;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.Principal;

import com.armedia.cmf.engine.cmis.CmisProperty;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.tools.AclTools;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
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
	protected Collection<CmisExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		CmisExportContext ctx) throws Exception {
		// TODO: identify the users/groups that need to be marshaled.
		// TODO: How the hell to list the members of groups?
		// TODO: How the hell to identify if an accessor is a user or a group?
		return super.identifyRequirements(marshalled, ctx);
	}

	@Override
	protected boolean marshal(CmisExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		// Copy the ACL Data into the object's attributes using the common ACL attributes
		final Acl acl = this.object.getAcl();

		CmfProperty<CmfValue> owner = new CmfProperty<CmfValue>(IntermediateProperty.ACL_OWNER, CmfDataType.STRING,
			false);
		owner.setValue(new CmfValue(this.object.getCreatedBy()));
		CmfProperty<CmfValue> name = new CmfProperty<CmfValue>(IntermediateProperty.ACL_OBJECT_ID, CmfDataType.STRING,
			false);
		name.setValue(new CmfValue(this.object.getId()));

		if (acl != null) {
			String permissionsName = String.format(CmisProperty.PERMISSION_PROPERTY_FMT, ctx.getRepositoryInfo()
				.getProductName().toLowerCase());
			CmfProperty<CmfValue> accessors = new CmfProperty<CmfValue>(IntermediateProperty.ACL_ACCESSOR_NAME,
				CmfDataType.STRING, true);
			CmfProperty<CmfValue> permissions = new CmfProperty<CmfValue>(permissionsName, CmfDataType.STRING, true);
			CmfProperty<CmfValue> accessorActions = new CmfProperty<CmfValue>(
				IntermediateProperty.ACL_ACCESSOR_ACTIONS, CmfDataType.STRING, true);

			for (Ace ace : acl.getAces()) {
				// Only export directly-applied ACEs
				if (!ace.isDirect()) {
					continue;
				}

				Set<String> actions = new TreeSet<String>();
				for (String permission : ace.getPermissions()) {
					actions.addAll(ctx.convertPermissionToAllowableActions(permission));
				}
				Principal p = ace.getPrincipal();

				// Ok...so now we have the principal, and the list of allowable actions that should
				// be permitted. Therefore, we can export this information
				accessors.addValue(new CmfValue(p.getId()));
				permissions.addValue(new CmfValue(AclTools.encode(ace.getPermissions())));
				accessorActions.addValue(new CmfValue(AclTools.encode(actions)));
			}

			object.setProperty(accessors);
			object.setProperty(permissions);
			object.setProperty(accessorActions);
			// TODO: Export extensions
		}
		object.setProperty(owner);
		object.setProperty(name);
		return true;
	}
}