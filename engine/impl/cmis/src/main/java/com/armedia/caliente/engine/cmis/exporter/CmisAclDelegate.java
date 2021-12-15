/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
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
		List<String> paths = this.factory.getPaths(object);
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
		owner.setValue(CmfValue.of(this.object.getCreatedBy()));
		CmfProperty<CmfValue> name = new CmfProperty<>(IntermediateProperty.ACL_OBJECT_ID, CmfValue.Type.STRING, false);
		name.setValue(CmfValue.of(this.object.getId()));

		if (acl != null) {
			CmfProperty<CmfValue> accessors = new CmfProperty<>(IntermediateProperty.ACL_ACCESSOR_NAME,
				CmfValue.Type.STRING, true);
			CmfProperty<CmfValue> permissions = new CmfProperty<>(IntermediateProperty.ACL_PERMISSION_NAME,
				CmfValue.Type.STRING, true);
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
				accessors.addValue(CmfValue.of(p.getId()));
				permissions.addValue(CmfValue.of(AclTools.encode(ace.getPermissions())));
				accessorActions.addValue(CmfValue.of(AclTools.encode(actions)));
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