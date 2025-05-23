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
package com.armedia.caliente.engine.cmis.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.MutableAce;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlEntryImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlPrincipalDataImpl;
import org.apache.groovy.parser.antlr4.util.StringUtils;

import com.armedia.caliente.engine.cmis.CmisProperty;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.importer.ImportResult;
import com.armedia.caliente.engine.tools.AclTools;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.tools.DefaultCmfObjectHandler;

public abstract class CmisFileableDelegate<T extends FileableCmisObject> extends CmisImportDelegate<T> {

	public CmisFileableDelegate(CmisImportDelegateFactory factory, Class<T> klass, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, klass, storedObject);
	}

	protected void applyAcl(final CmisImportContext ctx, final T object) throws ImportException {
		// Make sure that if ACL processing is disabled, we don't process it
		if (!ctx.isSupported(CmfObject.Archetype.ACL)) { return; }
		CmfProperty<CmfValue> aclIdAtt = this.cmfObject.getProperty(IntermediateProperty.ACL_ID);
		if ((aclIdAtt == null) || !aclIdAtt.hasValues()) { return; }
		CmfValue aclId = aclIdAtt.getValue();
		if ((aclId == null) || aclId.isNull()) { return; }

		final String permissionPropertyName = String.format(CmisProperty.PERMISSION_PROPERTY_FMT,
			ctx.getRepositoryInfo().getProductName().toLowerCase());

		CmfObjectHandler<CmfValue> handler = new DefaultCmfObjectHandler<CmfValue>() {

			@Override
			public boolean handleObject(CmfObject<CmfValue> dataObject) throws CmfStorageException {
				List<Ace> aces = object.getAcl().getAces();
				aces.clear();
				CmfProperty<CmfValue> accessorNames = dataObject.getProperty(IntermediateProperty.ACL_ACCESSOR_NAME);
				CmfProperty<CmfValue> directPermissions = dataObject.getProperty(permissionPropertyName);
				CmfProperty<CmfValue> accessorActions = dataObject
					.getProperty(IntermediateProperty.ACL_ACCESSOR_ACTIONS);

				if ((accessorNames == null) || ((accessorActions == null) && (directPermissions == null))) {
					return false;
				}

				if (directPermissions != null) {
					if (accessorNames.getValueCount() != directPermissions.getValueCount()) {
						CmisFileableDelegate.this.log.warn(
							"ACL accessors and directPermissions have different object counts ({} != {}) for {}",
							accessorNames.getValueCount(), accessorActions.getValueCount(),
							CmisFileableDelegate.this.cmfObject.getDescription());
					} else {
						// Ok...we have explicit permissions for each accessor, so skip the actions
						// mappings and simply apply them directly
						final int count = accessorNames.getValueCount();
						for (int i = 0; i < count; i++) {
							CmfValue accessor = accessorNames.getValue(i);
							if ((accessor == null) || accessor.isNull()) {
								continue;
							}
							CmfValue permissions = directPermissions.getValue(i);
							if ((permissions == null) || permissions.isNull()) {
								continue;
							}

							MutableAce ace = new AccessControlEntryImpl();
							ace.setDirect(true);
							ace.setPermissions(new ArrayList<>(AclTools.decode(permissions.asString())));
							ace.setPrincipal(new AccessControlPrincipalDataImpl(accessor.asString()));
							// TODO: Copy extensions!!
							aces.add(ace);
						}

						object.setAcl(aces);
						return false;
					}
				}

				if (accessorNames.getValueCount() != accessorActions.getValueCount()) {
					throw new CmfStorageException(
						String.format("ACL accessors and actions have different object counts (%,d != %,d) for %s",
							accessorNames.getValueCount(), accessorActions.getValueCount(),
							CmisFileableDelegate.this.cmfObject.getDescription()));
				}

				final int count = accessorNames.getValueCount();
				for (int i = 0; i < count; i++) {
					CmfValue accessor = accessorNames.getValue(i);
					if ((accessor == null) || accessor.isNull()) {
						continue;
					}
					CmfValue actions = accessorActions.getValue(i);
					if ((actions == null) || actions.isNull()) {
						continue;
					}
					Set<String> permissions = ctx
						.convertAllowableActionsToPermissions(AclTools.decode(actions.asString()));
					MutableAce ace = new AccessControlEntryImpl();
					ace.setDirect(true);
					ace.setPermissions(new ArrayList<>(permissions));
					ace.setPrincipal(new AccessControlPrincipalDataImpl(accessor.asString()));
					aces.add(ace);
				}

				object.setAcl(aces);
				return false;
			}

		};
		try {
			int count = ctx.loadObjects(CmfObject.Archetype.ACL, Collections.singleton(aclId.asString()), handler);
			if (count == 0) { return; }
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to load the ACL [%s] associated with %s", aclIdAtt,
				this.cmfObject.getDescription()), e);
		}
	}

	protected List<Folder> getParentFolders(CmisImportContext ctx) throws ImportException {

		final List<Folder> ret = new ArrayList<>();
		final Session session = ctx.getSession();

		// CMIS doesn't allow multi-filing, so we can only search for one path.
		String path = getFixedPath(ctx);
		// If we've truncated past our limit, we puke out
		if (path == null) { return null; }

		// Check to see if we have to return the root folder
		if (StringUtils.isEmpty(path) || "/".equals(path)) {
			ret.add(session.getRootFolder());
		} else {
			CmisObject obj = session.getObjectByPath(path);
			if (Folder.class.isInstance(obj)) {
				ret.add(Folder.class.cast(obj));
			}
		}

		return ret;
	}

	protected T findExisting(CmisImportContext ctx, List<Folder> parents) throws ImportException {
		final Session session = ctx.getSession();
		CmfAttribute<CmfValue> att = this.cmfObject.getAttribute(PropertyIds.NAME);
		if ((att == null) || !att.hasValues()) { return null; }
		CmfValue v = att.getValue();
		if (v.isNull()) { return null; }
		final String name = v.asString();
		for (Folder p : parents) {
			try {
				CmisObject obj = session.getObjectByPath(String.format("%s/%s", p.getPath(), name));
				if (this.objectClass.isInstance(obj)) { return this.objectClass.cast(obj); }
			} catch (CmisObjectNotFoundException e) {
				// Do nothing...
				continue;
			}
		}
		return null;
	}

	protected abstract T createNew(CmisImportContext ctx, Folder parent, Map<String, Object> properties)
		throws ImportException;

	protected void updateExisting(CmisImportContext ctx, T existing, Map<String, Object> properties)
		throws ImportException {
		existing.updateProperties(properties);
	}

	protected T createNewVersion(CmisImportContext ctx, T existing, Map<String, Object> properties)
		throws ImportException {
		return existing;
	}

	protected boolean isVersionable(T existing) {
		return false;
	}

	protected boolean isSameObject(T existing) {
		// Calendar creationDate = existing.getCreationDate();
		// Calendar modificationDate = existing.getLastModificationDate();
		return true;
	}

	protected abstract boolean isMultifilable(T existing);

	protected String calculateNewLabel(T existing) {
		List<String> paths = existing.getPaths();
		String versionLabel = "";
		if (isVersionable(existing)) {
			versionLabel = String.format("#%s", existing.getProperty(PropertyIds.VERSION_LABEL).getValueAsString());
		}
		if ((paths == null) || paths.isEmpty()) {
			return String.format("<unfiled>::%s%s", existing.getName(), versionLabel);
		}
		String path = paths.get(0);
		return String.format("%s%s", path, versionLabel);
	}

	protected void linkToParents(CmisImportContext ctx, T existing, List<Folder> finalParents) {
		Map<String, Folder> oldParents = new HashMap<>();
		Map<String, Folder> newParents = new HashMap<>();
		for (Folder f : finalParents) {
			newParents.put(f.getId(), f);
		}
		for (Folder f : existing.getParents()) {
			oldParents.put(f.getId(), f);
		}
		Set<String> oldParentsIds = new HashSet<>(oldParents.keySet());
		Set<String> newParentsIds = new HashSet<>(newParents.keySet());
		Set<String> bothParentsIds = new HashSet<>();

		// Remove those that need not be unlinked
		for (String s : newParentsIds) {
			oldParents.remove(s);
		}

		// Remove those that need not be re-linked
		for (String s : oldParentsIds) {
			newParents.remove(s);
		}
		bothParentsIds.addAll(oldParentsIds);
		bothParentsIds.retainAll(newParentsIds);

		final boolean multifile = ctx.getRepositoryInfo().getCapabilities().isMultifilingSupported();
		// && isMultifilable(existing);

		// Unlink from those no longer needed
		for (String s : oldParents.keySet()) {
			existing.removeFromFolder(oldParents.get(s));
		}

		if (multifile || bothParentsIds.isEmpty()) {
			// Link to those needed but not yet linked
			for (String s : newParents.keySet()) {
				// TODO: "allVersions" may not be supported by the target ... how can
				// we identify this ahead of time?
				existing.addToFolder(newParents.get(s), true);
				if (!multifile) {
					break;
				}
			}
		}

		if (!newParents.isEmpty() || !oldParents.isEmpty()) {
			existing.refresh();
		}
	}

	private void setMapping(CmisImportContext ctx, T existing) {
		ctx.getValueMapper().setMapping(this.cmfObject.getType(), PropertyIds.OBJECT_ID, this.cmfObject.getId(),
			existing.getId());
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, CmisImportContext ctx)
		throws ImportException, CmfStorageException {
		boolean ok = false;
		boolean created = false;
		T existing = null;
		try {
			Map<String, Object> props = prepareProperties(translator, ctx);
			props.remove(PropertyIds.PATH);
			props.remove(PropertyIds.PARENT_ID);

			List<Folder> parents = getParentFolders(ctx);
			if ((parents == null) || parents.isEmpty()) { return Collections.singleton(ImportOutcome.SKIPPED); }

			// Find the parent folder...
			final Folder parent = parents.get(0);

			// First, try to find the existing object.
			existing = findExisting(ctx, parents);
			if (existing == null) {
				// If it doesn't exist, we'll create the new object...
				existing = createNew(ctx, parent, props);
				created = true;
				applyAcl(ctx, existing);
				linkToParents(ctx, existing, parents);
				setMapping(ctx, existing);
				return Collections
					.singleton(new ImportOutcome(ImportResult.CREATED, existing.getId(), calculateNewLabel(existing)));
			}

			if (isSameObject(existing)) {
				return Collections.singleton(
					new ImportOutcome(ImportResult.DUPLICATE, existing.getId(), calculateNewLabel(existing)));
			}
			if (isVersionable(existing)) {
				existing = createNewVersion(ctx, existing, props);
				applyAcl(ctx, existing);
				linkToParents(ctx, existing, parents);
				setMapping(ctx, existing);
				return Collections
					.singleton(new ImportOutcome(ImportResult.CREATED, existing.getId(), calculateNewLabel(existing)));
			}

			// Not the same...we must update the properties and/or content
			updateExisting(ctx, existing, props);
			applyAcl(ctx, existing);
			linkToParents(ctx, existing, parents);
			setMapping(ctx, existing);
			return Collections
				.singleton(new ImportOutcome(ImportResult.UPDATED, existing.getId(), calculateNewLabel(existing)));
		} finally {
			// If we're required to delete this object upon failure, do so ...
			if (!ok && (existing != null) && this.factory.isDeleteOnFail()) {
				try {
					ctx.getSession().delete(existing, true);
				} catch (Exception e) {
					this.log.warn("DeleteOnFail is set, but was unable to delete the {} object for {} at [{}]",
						created ? "created" : "existing", this.cmfObject.getDescription(), existing, e);
				}
			}
		}
	}
}