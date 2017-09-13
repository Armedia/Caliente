package com.armedia.caliente.engine.ucm.model;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataObject;

public class UcmFSObject {

	private static final String ROOT_GUID = "FLD_ROOT";

	protected final DataObject data;
	protected UcmFolder parent = null;
	private final UcmAtt guidAtt;
	private final UcmAtt nameAtt;
	private final String path;

	public UcmFSObject(DataObject data, UcmAtt nameAtt, UcmAtt guidAtt) {
		this(null, data, nameAtt, guidAtt);
	}

	protected UcmFSObject(UcmFolder parent, DataObject data, UcmAtt nameAtt, UcmAtt guidAtt) {
		this.parent = parent;
		this.data = data;
		this.nameAtt = nameAtt;
		this.guidAtt = guidAtt;

		String guid = getString(guidAtt);
		if (guid.equals(UcmFSObject.ROOT_GUID)) {
			// Special case for the root folder
			this.path = "/";
		} else {
			String prefix = (parent != null ? parent.getPath() : "<unknown>");
			String slash = (prefix.endsWith("/") ? "" : "/");
			this.path = String.format("%s%s%s", prefix, slash, getString(nameAtt));
		}
	}

	public String getPath() {
		return this.path;
	}

	public boolean isParentLoaded() {
		return this.parent != null;
	}

	public UcmFolder getParentFolder(IdcSession session) throws IdcClientException {
		return null;
	}

	protected final String getString(UcmAtt att) {
		return getString(att, null);
	}

	protected final String getString(UcmAtt att, String def) {
		return Tools.coalesce(this.data.get(att.name()), def);
	}

	protected final Date getDate(UcmAtt att) {
		return getDate(att, null);
	}

	protected final Date getDate(UcmAtt att, Date def) {
		return Tools.coalesce(this.data.getDate(att.name()), def);
	}

	protected final Calendar getCalendar(UcmAtt att) {
		return getCalendar(att, null);
	}

	protected final Calendar getCalendar(UcmAtt att, Calendar def) {
		return Tools.coalesce(this.data.getCalendar(att.name()), def);
	}

	protected final Integer getInteger(UcmAtt att) {
		return this.data.getInteger(att.name());
	}

	protected final int getInteger(UcmAtt att, int def) {
		Integer v = getInteger(att);
		return (v != null ? v.intValue() : def);
	}

	protected final Boolean getBoolean(UcmAtt att) {
		return Tools.toBoolean(getString(att));
	}

	protected final boolean getBoolean(UcmAtt att, boolean def) {
		Boolean b = getBoolean(att);
		return (b != null ? b.booleanValue() : def);
	}

	public final String getGUID() {
		return getString(this.guidAtt);
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

	public final String getParentGUID() {
		return getString(UcmAtt.fParentGUID);
	}

	public final String getSecurityGroup() {
		return getString(UcmAtt.fSecurityGroup);
	}

	public DataObject getDataObject() {
		return this.data;
	}

	public boolean isShortcut() {
		return !StringUtils.isEmpty(getString(UcmAtt.fTargetGUID));
	}

	@Override
	public String toString() {
		return String.format("%s [guid=%s, path=%s]", getClass().getSimpleName(), getGUID(), this.path);
	}
}