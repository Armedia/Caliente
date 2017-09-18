package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.ucm.UcmSession;

public abstract class UcmFSObject extends UcmModelObject {

	protected final UcmAtt nameAtt;

	private final UcmAttributes attributes;
	private final String path;
	private final String parentPath;

	private final UcmUniqueURI uniqueUri;
	private final UcmUniqueURI parentUri;

	UcmFSObject(UcmModel model, URI uri, UcmAttributes data, UcmAtt nameAtt) {
		super(model, uri);
		// Here we use the cloning constructor so we keep a *copy* of the DataObject, to allow
		// the caches in the model the opportunity to expire objects appropriately regardless
		// of references held outside the model
		this.attributes = data;
		this.nameAtt = nameAtt;
		this.parentPath = this.attributes.getString(UcmAtt.$ucmParentPath);
		if (this.parentPath.equals("/")) {
			this.path = String.format("/%s", this.attributes.getString(nameAtt));
		} else {
			this.path = String.format("%s/%s", this.parentPath, this.attributes.getString(nameAtt));
		}
		this.uniqueUri = UcmModel.getUniqueURI(data);
		this.parentUri = new UcmUniqueURI(UcmModel.newFolderURI(getString(UcmAtt.fParentGUID)));
	}

	public final String getString(UcmAtt att) {
		return this.attributes.getString(att);
	}

	public final String getString(UcmAtt att, String def) {
		return this.attributes.getString(att, def);
	}

	public final Date getDate(UcmAtt att) {
		return this.attributes.getDate(att);
	}

	public final Date getDate(UcmAtt att, Date def) {
		return this.attributes.getDate(att, def);
	}

	public final Calendar getCalendar(UcmAtt att) {
		return this.attributes.getCalendar(att);
	}

	public final Calendar getCalendar(UcmAtt att, Calendar def) {
		return this.attributes.getCalendar(att, def);
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

	public final UcmAttributes getAttribites() {
		return this.attributes;
	}

	public String getPath() {
		return this.path;
	}

	public String getParentPath() {
		return this.parentPath;
	}

	public UcmFolder getParentFolder(UcmSession s) throws UcmFolderNotFoundException, UcmServiceException {
		return this.model.getFolder(s, getParentURI());
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

	public final Date getCreationDate() {
		return getDate(UcmAtt.fCreateDate);
	}

	public final String getCreator() {
		return getString(UcmAtt.fCreator);
	}

	public final Date getLastModifiedDate() {
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

	public boolean isShortcut() {
		return !StringUtils.isEmpty(getString(UcmAtt.fTargetGUID));
	}
}