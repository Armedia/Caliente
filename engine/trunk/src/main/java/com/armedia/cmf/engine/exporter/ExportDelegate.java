package com.armedia.cmf.engine.exporter;

import java.util.Collection;
import java.util.List;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.SessionWrapper;
import com.armedia.cmf.engine.TransferDelegate;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportDelegate<T, S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V>, E extends ExportEngine<S, W, V, C>>
	extends TransferDelegate<T, S, V, E> {

	protected ExportDelegate(E engine, Class<T> objectClass, T object, CfgTools configuration) throws Exception {
		super(engine, objectClass, object, configuration);
	}

	protected abstract Collection<? extends ExportDelegate<?, S, W, V, C, ?>> identifyRequirements(
		StoredObject<V> marshalled, C ctx) throws Exception;

	final StoredObject<V> marshal(C ctx, ExportTarget referrent) throws ExportException {
		StoredObjectType type = getType();
		StoredObject<V> marshaled = new StoredObject<V>(type, getObjectId(), getSearchKey(), getBatchId(), getLabel(),
			type.name());
		marshal(ctx, marshaled);
		this.engine.setReferrent(marshaled, referrent);
		return marshaled;
	}

	protected abstract void marshal(C ctx, StoredObject<V> object) throws ExportException;

	protected abstract Collection<? extends ExportDelegate<?, S, W, V, C, ?>> identifyDependents(
		StoredObject<V> marshalled, C ctx) throws Exception;

	protected abstract List<ContentInfo> storeContent(S session, StoredObject<V> marshalled, ExportTarget referrent,
		ContentStore streamStore) throws Exception;
}