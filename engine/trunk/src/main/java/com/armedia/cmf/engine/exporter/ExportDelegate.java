package com.armedia.cmf.engine.exporter;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.Tools;

public abstract class ExportDelegate<S, W extends SessionWrapper<S>, T, V, C extends ExportContext<S, T, V>> {
	private static final String REFERRENT_ID = "${REFERRENT_ID}$";
	private static final String REFERRENT_KEY = "${REFERRENT_KEY}$";
	private static final String REFERRENT_TYPE = "${REFERRENT_TYPE}$";

	private final ObjectStorageTranslator<T, V> translator;
	private final T object;
	private final ExportTarget exportTarget;

	protected ExportDelegate(ObjectStorageTranslator<T, V> translator, T object) {
		if (object == null) { throw new IllegalArgumentException("Must provide an object to process"); }
		this.translator = translator;
		this.object = object;
		this.exportTarget = new ExportTarget(getType(), getObjectId(), getSearchKey());
	}

	public final ObjectStorageTranslator<T, V> getTranslator() {
		return this.translator;
	}

	public final T getObject() {
		return this.object;
	}

	public final ExportTarget getExportTarget() {
		return this.exportTarget;
	}

	public abstract StoredObjectType getType();

	public abstract String calculateLabel();

	public abstract String getObjectId();

	public abstract String getSearchKey();

	protected abstract Collection<ExportDelegate<S, W, ?, V, C>> identifyRequirements(S session,
		StoredObject<V> marshalled, C ctx) throws Exception;

	final StoredObject<V> marshal(C ctx, S session, ExportTarget referrent) throws ExportException {
		StoredObject<V> marshaled = marshal(ctx, session);
		// Now, add the properties to reference the referrent object
		if (referrent != null) {
			StoredProperty<V> referrentType = new StoredProperty<V>(ExportDelegate.REFERRENT_TYPE,
				StoredDataType.STRING, false);
			try {
				referrentType.setValue(this.translator.getValue(StoredDataType.STRING, referrent.getType().name()));
				marshaled.setProperty(referrentType);
				StoredProperty<V> referrentId = new StoredProperty<V>(ExportDelegate.REFERRENT_ID,
					StoredDataType.STRING, false);
				referrentId.setValue(this.translator.getValue(StoredDataType.STRING, referrent.getId()));
				marshaled.setProperty(referrentId);
				StoredProperty<V> referrentKey = new StoredProperty<V>(ExportDelegate.REFERRENT_KEY,
					StoredDataType.STRING, false);
				referrentId.setValue(this.translator.getValue(StoredDataType.STRING, referrent.getSearchKey()));
				marshaled.setProperty(referrentKey);
			} catch (ParseException e) {
				// This should never happen...
				throw new ExportException("Failed to store the referrent information", e);
			}
		}
		return marshaled;
	}

	protected abstract StoredObject<V> marshal(C ctx, S session) throws ExportException;

	protected abstract Collection<ExportDelegate<S, W, ?, V, C>> identifyDependents(S session,
		StoredObject<V> marshalled, C ctx) throws Exception;

	protected abstract List<ContentInfo> storeContent(S session, StoredObject<V> marshalled, ExportTarget referrent,
		ContentStore streamStore) throws Exception;

	final ExportTarget getReferrent(StoredObject<V> marshaled) {
		if (marshaled == null) { throw new IllegalArgumentException("Must provide a marshaled object to analyze"); }
		StoredProperty<V> referrentType = marshaled.getProperty(ExportDelegate.REFERRENT_TYPE);
		StoredProperty<V> referrentId = marshaled.getProperty(ExportDelegate.REFERRENT_ID);
		StoredProperty<V> referrentKey = marshaled.getProperty(ExportDelegate.REFERRENT_KEY);
		if ((referrentType == null) || (referrentId == null) || (referrentKey == null)) { return null; }
		String type = Tools.toString(referrentType.getValue(), true);
		String id = Tools.toString(referrentId.getValue(), true);
		String key = Tools.toString(referrentKey.getValue(), true);
		if ((type == null) || (id == null) || (key == null)) { return null; }
		return new ExportTarget(StoredObjectType.decodeString(type), id, key);
	}
}