package com.armedia.caliente.engine.cmis.exporter;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.Principal;

import com.armedia.caliente.engine.cmis.CmisProperty;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.tools.AclTools;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;

public class CmisAclDelegate extends CmisExportDelegate<FileableCmisObject> {

	protected CmisAclDelegate(CmisExportDelegateFactory factory, Session session, FileableCmisObject object)
		throws Exception {
		super(factory, session, FileableCmisObject.class, object);
	}

	@Override
	protected CmfObject.Archetype calculateType(Session session, FileableCmisObject object) throws Exception {
		return CmfObject.Archetype.ACL;
	}

	@Override
	protected String calculateLabel(Session session, FileableCmisObject object) throws Exception {
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
	protected String calculateObjectId(Session session, FileableCmisObject object) throws Exception {
		return String.format("ACL-[%s]", object.getId());
	}

	@Override
	protected String calculateSearchKey(Session session, FileableCmisObject object) throws Exception {
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

		CmfProperty<CmfValue> owner = new CmfProperty<>(IntermediateProperty.ACL_OWNER, CmfValue.Type.STRING, false);
		owner.setValue(new CmfValue(this.object.getCreatedBy()));
		CmfProperty<CmfValue> name = new CmfProperty<>(IntermediateProperty.ACL_OBJECT_ID, CmfValue.Type.STRING, false);
		name.setValue(new CmfValue(this.object.getId()));

		if (acl != null) {
			String permissionsName = String.format(CmisProperty.PERMISSION_PROPERTY_FMT,
				ctx.getRepositoryInfo().getProductName().toLowerCase());
			CmfProperty<CmfValue> accessors = new CmfProperty<>(IntermediateProperty.ACL_ACCESSOR_NAME,
				CmfValue.Type.STRING, true);
			CmfProperty<CmfValue> permissions = new CmfProperty<>(permissionsName, CmfValue.Type.STRING, true);
			CmfProperty<CmfValue> accessorActions = new CmfProperty<>(IntermediateProperty.ACL_ACCESSOR_ACTIONS,
				CmfValue.Type.STRING, true);

			for (Ace ace : acl.getAces()) {
				// Only export directly-applied ACEs
				if (!ace.isDirect()) {
					continue;
				}

				Set<String> actions = new TreeSet<>();
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

	@Override
	protected String calculateName(Session session, FileableCmisObject aclObject) throws Exception {
		return aclObject.getId();
	}
}