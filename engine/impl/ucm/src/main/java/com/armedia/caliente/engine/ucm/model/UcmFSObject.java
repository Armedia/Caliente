package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import oracle.stellent.ridc.model.DataObject;

public abstract class UcmFSObject extends UcmModelObject {

	protected final UcmAtt guidAtt;
	protected final UcmAtt nameAtt;

	private final UcmTools dataObject;
	private final String path;
	private final String parentPath;

	UcmFSObject(UcmModel model, URI uri, DataObject data, UcmAtt nameAtt, UcmAtt guidAtt) {
		super(model, uri);
		// Here we use the cloning constructor so we keep a *copy* of the DataObject, to allow
		// the caches in the model the opportunity to expire objects appropriately regardless
		// of references held outside the model
		this.dataObject = new UcmTools(data, true);
		this.nameAtt = nameAtt;
		this.guidAtt = guidAtt;
		this.parentPath = this.dataObject.getString(UcmAtt.$ucmParentPath);
		if (this.parentPath.equals("/")) {
			this.path = String.format("/%s", this.dataObject.getString(nameAtt));
		} else {
			this.path = String.format("%s/%s", this.parentPath, this.dataObject.getString(nameAtt));
		}
	}

	public final UcmGUID getGUID(UcmAtt att) {
		return getGUID(att, null);
	}

	public final UcmGUID getGUID(UcmAtt att, UcmGUID def) {
		String str = this.dataObject.getString(att);
		return (str != null ? new UcmGUID(str) : def);
	}

	public final String getString(UcmAtt att) {
		return this.dataObject.getString(att);
	}

	public final String getString(UcmAtt att, String def) {
		return this.dataObject.getString(att, def);
	}

	public final Date getDate(UcmAtt att) {
		return this.dataObject.getDate(att);
	}

	public final Date getDate(UcmAtt att, Date def) {
		return this.dataObject.getDate(att, def);
	}

	public final Calendar getCalendar(UcmAtt att) {
		return this.dataObject.getCalendar(att);
	}

	public final Calendar getCalendar(UcmAtt att, Calendar def) {
		return this.dataObject.getCalendar(att, def);
	}

	public final Integer getInteger(UcmAtt att) {
		return this.dataObject.getInteger(att);
	}

	public final int getInteger(UcmAtt att, int def) {
		return this.dataObject.getInteger(att, def);
	}

	public final Boolean getBoolean(UcmAtt att) {
		return this.dataObject.getBoolean(att);
	}

	public final boolean getBoolean(UcmAtt att, boolean def) {
		return this.dataObject.getBoolean(att, def);
	}

	public final DataObject getDataObject() {
		return this.dataObject.getDataObject();
	}

	public String getPath() {
		return this.path;
	}

	public String getParentPath() {
		return this.parentPath;
	}

	public UcmFolder getParentFolder() throws UcmFolderNotFoundException, UcmServiceException {
		return this.model.getFolder(getParentGUID());
	}

	public final UcmGUID getObjectGUID() {
		return getGUID(this.guidAtt);
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

	public final UcmGUID getParentGUID() {
		return getGUID(UcmAtt.fParentGUID);
	}

	public final String getSecurityGroup() {
		return getString(UcmAtt.fSecurityGroup);
	}

	public boolean isShortcut() {
		return !StringUtils.isEmpty(getString(UcmAtt.fTargetGUID));
	}
}