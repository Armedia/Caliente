package com.armedia.caliente.engine.ucm.model;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataObject;

public abstract class UcmFSObject extends UcmModelObject {

	private class PathInitializer extends LazyInitializer<String> {

		@Override
		protected String initialize() throws ConcurrentException {
			if (isRootFolder()) { return UcmFSObject.ROOT_PATH; }
			try {
				String prefix = getParentFolder().getPath();
				String slash = (prefix.endsWith("/") ? "" : "/");
				return String.format("%s%s%s", prefix, slash, getString(UcmFSObject.this.nameAtt));
			} catch (IdcClientException e) {
				throw new ConcurrentException(String.format("Failed to retrieve the parent folder for GUID [%s] (%s)"),
					e);
			}
		}

	}

	private static final String ROOT_PATH = "/";
	private static final String ROOT_GUID = "FLD_ROOT";

	protected final UcmAtt guidAtt;
	protected final UcmAtt nameAtt;

	private final DataObject data;

	private LazyInitializer<String> path = new PathInitializer();

	UcmFSObject(UcmModel model, DataObject data, UcmAtt nameAtt, UcmAtt guidAtt) {
		super(model);
		this.data = data;
		this.nameAtt = nameAtt;
		this.guidAtt = guidAtt;
	}

	private boolean isRootFolder() {
		return Tools.equals(getGUID(), UcmFSObject.ROOT_GUID);
	}

	public String getPath() throws IdcClientException {
		try {
			return this.path.get();
		} catch (ConcurrentException e) {
			Throwable t = e.getCause();
			if (IdcClientException.class.isInstance(t)) { throw IdcClientException.class.cast(t); }
			throw new IdcRuntimeException(e.getMessage(), t);
		}
	}

	public UcmFolder getParentFolder() throws IdcClientException {
		return this.model.getFolderByGUID(getParentGUID());
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
}