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
package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.store.CmfValue;

public abstract class UcmFSObject extends UcmModelObject {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final UcmAtt nameAtt;

	private final UcmAttributes attributes;
	private final String path;
	private final String parentPath;

	private final UcmUniqueURI uniqueUri;
	private final UcmUniqueURI parentUri;

	private final UcmObjectType ucmObjectType;
	private final boolean unfiled;

	UcmFSObject(UcmModel model, URI uri, UcmAttributes data, UcmAtt... nameAtts) {
		super(model, uri);
		Objects.requireNonNull(data, String.format("No attribute data provided for URI [%s]", uri));
		// Here we use the cloning constructor so we keep a *copy* of the DataObject, to allow
		// the caches in the model the opportunity to expire objects appropriately regardless
		// of references held outside the model
		this.uniqueUri = UcmModel.getUniqueURI(data);
		URI parentUri = UcmModel.NULL_FOLDER_URI;
		final Map<String, CmfValue> mutableData = data.getMutableData();
		if (data.hasAttribute(UcmAtt.fParentGUID)) {
			parentUri = UcmModel.newFolderURI(data.getString(UcmAtt.fParentGUID));
			if (UcmModel.isRoot(parentUri)) {
				parentUri = UcmModel.NULL_FOLDER_URI;
				mutableData.put(UcmAtt.fParentGUID.name(), CmfValue.of(parentUri.getSchemeSpecificPart()));
			}
		}
		this.parentUri = new UcmUniqueURI(parentUri);
		mutableData.put(UcmAtt.cmfUniqueURI.name(), CmfValue.of(this.uniqueUri.toString()));
		mutableData.put(UcmAtt.cmfParentURI.name(), CmfValue.of(this.parentUri.toString()));

		this.attributes = data;
		UcmAtt nameAtt = null;
		for (UcmAtt att : nameAtts) {
			if (data.hasAttribute(att)) {
				nameAtt = att;
				break;
			}
		}
		if (nameAtt == null) {
			throw new IllegalArgumentException(
				String.format("No object name attribute is present from among %s", Arrays.toString(nameAtts)));
		}
		this.nameAtt = nameAtt;

		this.parentPath = this.attributes.getString(UcmAtt.cmfParentPath);
		if (this.parentPath != null) {
			String name = this.attributes.getString(this.nameAtt);
			if (this.parentPath.equals("/")) {
				if (name.equals("/")) {
					name = "";
				}
				this.path = String.format("/%s", name);
			} else {
				this.path = String.format("%s/%s", this.parentPath, name);
			}
			mutableData.put(UcmAtt.cmfPath.name(), CmfValue.of(this.path));
			this.unfiled = false;
		} else {
			this.path = String.format("{unfiled[#%08x]:%s}", this.attributes.getInteger(UcmAtt.dID),
				this.attributes.getString(UcmAtt.dOriginalName));
			this.unfiled = true;
		}

		this.ucmObjectType = (UcmModel.isFileURI(uri) ? UcmObjectType.FILE : UcmObjectType.FOLDER);
		this.log.debug("New {} for {} ({})", this.ucmObjectType.name(), this.uniqueUri, this.path);
	}

	public final boolean isUnfiled() {
		return this.unfiled;
	}

	public final UcmObjectType getType() {
		return this.ucmObjectType;
	}

	public final String getString(UcmAtt att) {
		return this.attributes.getString(att);
	}

	public final String getString(UcmAtt att, String def) {
		return this.attributes.getString(att, def);
	}

	public final Date getDate(UcmAtt att) throws ParseException {
		return this.attributes.getDate(att);
	}

	public final Date getDate(UcmAtt att, Date def) throws ParseException {
		return this.attributes.getDate(att, def);
	}

	public final Long getLong(UcmAtt att) {
		return this.attributes.getLong(att);
	}

	public final long getLong(UcmAtt att, long def) {
		return this.attributes.getLong(att, def);
	}

	public final Integer getInteger(UcmAtt att) {
		return this.attributes.getInteger(att);
	}

	public final int getInteger(UcmAtt att, int def) {
		return this.attributes.getInteger(att, def);
	}

	public final Boolean getBoolean(UcmAtt att) {
		return this.attributes.getBoolean(att);
	}

	public final boolean getBoolean(UcmAtt att, boolean def) {
		return this.attributes.getBoolean(att, def);
	}

	public final UcmAttributes getAttributes() {
		return this.attributes;
	}

	public final Set<String> getAttributeNames() {
		return this.attributes.getValueNames();
	}

	public final CmfValue getValue(String name) {
		return this.attributes.getValue(name);
	}

	public final CmfValue getValue(UcmAtt att) {
		return this.attributes.getValue(att);
	}

	public String getPath() {
		return this.path;
	}

	public String getParentPath() {
		return this.parentPath;
	}

	public UcmFolder getParentFolder(UcmSession s) throws UcmFolderNotFoundException, UcmServiceException {
		return s.getParentFolder(this);
	}

	public final UcmUniqueURI getUniqueURI() {
		return this.uniqueUri;
	}

	public final String getName() {
		return getString(this.nameAtt);
	}

	public final String getDisplayName() {
		return getString(UcmAtt.fDisplayName);
	}

	public final String getOwner() {
		return getString(UcmAtt.fOwner);
	}

	public final Date getCreationDate() throws ParseException {
		return getDate(UcmAtt.fCreateDate);
	}

	public final String getCreator() {
		return getString(UcmAtt.fCreator);
	}

	public final Date getLastModifiedDate() throws ParseException {
		return getDate(UcmAtt.fLastModifiedDate);
	}

	public final String getLastModifier() {
		return getString(UcmAtt.fLastModifier);
	}

	public final boolean isInTrash() {
		return getBoolean(UcmAtt.fIsInTrash, false);
	}

	public final URI getParentURI() {
		return this.parentUri.getURI();
	}

	public final String getSecurityGroup() {
		return getString(UcmAtt.fSecurityGroup);
	}

	public final boolean isShortcut() {
		return !StringUtils.isEmpty(getTargetGUID());
	}

	public final String getTargetGUID() {
		return getString(UcmAtt.fTargetGUID);
	}

	public final boolean hasAttribute(UcmAtt attribute) {
		return this.attributes.hasAttribute(attribute);
	}

	public final boolean hasAttribute(String name) {
		return this.attributes.hasAttribute(name);
	}
}