package com.armedia.caliente.engine.ucm.model;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import com.armedia.commons.utilities.Tools;

import oracle.stellent.ridc.model.DataObject;

public abstract class UcmFSObject extends UcmModelObject {

	private class PathInitializer extends LazyInitializer<String> {

		@Override
		protected String initialize() throws ConcurrentException {
			try {
				if (isRootFolder()) { return UcmFSObject.ROOT_PATH; }
				String prefix = getParentFolder().getPath();
				String slash = (prefix.endsWith("/") ? "" : "/");
				return String.format("%s%s%s", prefix, slash, getString(UcmFSObject.this.nameAtt));
			} catch (UcmException e) {
				throw new ConcurrentException(
					String.format("Failed to retrieve the parent folder for UcmGUID [%s] (%s)"), e);
			}
		}

	}

	private static final String ROOT_PATH = "/";
	private static final String ROOT_GUID = "FLD_ROOT";

	protected final UcmAtt guidAtt;
	protected final UcmAtt nameAtt;

	private final UcmTools dataObject;

	private LazyInitializer<String> path = new PathInitializer();

	UcmFSObject(UcmModel model, DataObject data, UcmAtt nameAtt, UcmAtt guidAtt) {
		super(model);
		this.dataObject = new UcmTools(data);
		this.nameAtt = nameAtt;
		this.guidAtt = guidAtt;
	}

	private boolean isRootFolder() throws UcmException {
		return Tools.equals(getObjectGUID(), UcmFSObject.ROOT_GUID);
	}

	public final UcmGUID getGUID(UcmAtt att) throws UcmException {
		return getGUID(att, null);
	}

	public final UcmGUID getGUID(UcmAtt att, UcmGUID def) throws UcmException {
		String str = getFreshData().getString(att);
		return (str != null ? new UcmGUID(str) : def);
	}

	public final String getString(UcmAtt att) throws UcmException {
		return getFreshData().getString(att);
	}

	public final String getString(UcmAtt att, String def) throws UcmException {
		return getFreshData().getString(att, def);
	}

	public final Date getDate(UcmAtt att) throws UcmException {
		return getFreshData().getDate(att);
	}

	public final Date getDate(UcmAtt att, Date def) throws UcmException {
		return getFreshData().getDate(att, def);
	}

	public final Calendar getCalendar(UcmAtt att) throws UcmException {
		return getFreshData().getCalendar(att);
	}

	public final Calendar getCalendar(UcmAtt att, Calendar def) throws UcmException {
		return getFreshData().getCalendar(att, def);
	}

	public final Integer getInteger(UcmAtt att) throws UcmException {
		return getFreshData().getInteger(att);
	}

	public final int getInteger(UcmAtt att, int def) throws UcmException {
		return getFreshData().getInteger(att, def);
	}

	public final Boolean getBoolean(UcmAtt att) throws UcmException {
		return getFreshData().getBoolean(att);
	}

	public final boolean getBoolean(UcmAtt att, boolean def) throws UcmException {
		return getFreshData().getBoolean(att, def);
	}

	protected UcmTools getFreshData() {
		// TODO: Actually go to the model to fetch the latest version as required
		return this.dataObject;
	}

	public final DataObject getDataObject() {
		return getFreshData().getDataObject();
	}

	public String getPath() throws UcmException {
		try {
			return this.path.get();
		} catch (ConcurrentException e) {
			Throwable t = e.getCause();
			if (UcmException.class.isInstance(t)) { throw UcmException.class.cast(t); }
			throw new UcmRuntimeException(e.getMessage(), t);
		}
	}

	public UcmFolder getParentFolder() throws UcmException {
		return this.model.getFolder(getParentGUID());
	}

	public final UcmGUID getObjectGUID() throws UcmException {
		return getGUID(this.guidAtt);
	}

	public final String getName() throws UcmException {
		return getString(this.nameAtt);
	}

	public final String getDisplayName() throws UcmException {
		return getString(UcmAtt.fDisplayName);
	}

	public final String getOwner() throws UcmException {
		return getString(UcmAtt.fOwner);
	}

	public final Date getCreationDate() throws UcmException {
		return getDate(UcmAtt.fCreateDate);
	}

	public final String getCreator() throws UcmException {
		return getString(UcmAtt.fCreator);
	}

	public final Date getLastModifiedDate() throws UcmException {
		return getDate(UcmAtt.fLastModifiedDate);
	}

	public final String getLastModifier() throws UcmException {
		return getString(UcmAtt.fLastModifier);
	}

	public final boolean isInTrash() throws UcmException {
		return getBoolean(UcmAtt.fIsInTrash, false);
	}

	public final UcmGUID getParentGUID() throws UcmException {
		return getGUID(UcmAtt.fParentGUID);
	}

	public final String getSecurityGroup() throws UcmException {
		return getString(UcmAtt.fSecurityGroup);
	}

	public boolean isShortcut() throws UcmException {
		return !StringUtils.isEmpty(getString(UcmAtt.fTargetGUID));
	}
}