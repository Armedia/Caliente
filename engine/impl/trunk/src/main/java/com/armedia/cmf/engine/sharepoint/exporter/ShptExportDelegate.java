package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.exporter.ExportDelegate;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Folder;
import com.independentsoft.share.Group;
import com.independentsoft.share.User;

public abstract class ShptExportDelegate<T>
extends
ExportDelegate<T, ShptSession, ShptSessionWrapper, StoredValue, ShptExportContext, ShptExportDelegateFactory, ShptExportEngine> {

	private static final Map<Class<?>, StoredObjectType> TYPE_MAP;

	static {
		Map<Class<?>, StoredObjectType> m = new LinkedHashMap<Class<?>, StoredObjectType>();
		m.put(ShptVersion.class, StoredObjectType.DOCUMENT);
		m.put(Folder.class, StoredObjectType.FOLDER);
		m.put(Group.class, StoredObjectType.GROUP);
		m.put(User.class, StoredObjectType.USER);
		TYPE_MAP = Tools.freezeMap(m);
	}

	protected ShptExportDelegate(ShptExportDelegateFactory factory, Class<T> objectClass, T object) throws Exception {
		super(factory, objectClass, object);
	}

	@Override
	protected Collection<? extends ShptExportDelegate<?>> identifyRequirements(StoredObject<StoredValue> marshalled,
		ShptExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected boolean marshal(ShptExportContext ctx, StoredObject<StoredValue> object) throws ExportException {
		return true;
	}

	@Override
	protected Collection<? extends ShptExportDelegate<?>> identifyDependents(StoredObject<StoredValue> marshalled,
		ShptExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected List<ContentInfo> storeContent(ShptSession session, ObjectStorageTranslator<StoredValue> translator,
		StoredObject<StoredValue> marshalled, ExportTarget referrent, ContentStore<?> streamStore) throws Exception {
		return null;
	}

	@Override
	protected final StoredObjectType calculateType(T object) throws Exception {
		for (Map.Entry<Class<?>, StoredObjectType> e : ShptExportDelegate.TYPE_MAP.entrySet()) {
			if (e.getKey().isInstance(object)) { return e.getValue(); }
		}
		return null;
	}
}