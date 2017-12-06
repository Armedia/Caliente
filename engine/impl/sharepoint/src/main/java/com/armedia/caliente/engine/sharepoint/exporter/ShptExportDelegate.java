package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentInfo;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Folder;
import com.independentsoft.share.Group;
import com.independentsoft.share.User;

public abstract class ShptExportDelegate<T> extends
	ExportDelegate<T, ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportDelegateFactory, ShptExportEngine> {

	private static final Map<Class<?>, CmfType> TYPE_MAP;

	static {
		Map<Class<?>, CmfType> m = new LinkedHashMap<>();
		m.put(ShptVersion.class, CmfType.DOCUMENT);
		m.put(Folder.class, CmfType.FOLDER);
		m.put(Group.class, CmfType.GROUP);
		m.put(User.class, CmfType.USER);
		TYPE_MAP = Tools.freezeMap(m);
	}

	protected ShptExportDelegate(ShptExportDelegateFactory factory, ShptSession session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
	}

	@Override
	protected Collection<? extends ShptExportDelegate<?>> identifyRequirements(CmfObject<CmfValue> marshalled,
		ShptExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected boolean marshal(ShptExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		return true;
	}

	@Override
	protected Collection<? extends ShptExportDelegate<?>> identifyDependents(CmfObject<CmfValue> marshalled,
		ShptExportContext ctx) throws Exception {
		return null;
	}

	@Override
	protected List<CmfContentInfo> storeContent(ShptExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, ExportTarget referrent, CmfContentStore<?, ?, ?> streamStore,
		boolean includeRenditions) throws Exception {
		return null;
	}

	@Override
	protected final CmfType calculateType(ShptSession session, T object) throws Exception {
		for (Map.Entry<Class<?>, CmfType> e : ShptExportDelegate.TYPE_MAP.entrySet()) {
			if (e.getKey().isInstance(object)) { return e.getValue(); }
		}
		return null;
	}
}