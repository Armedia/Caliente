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
package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.armedia.caliente.engine.exporter.ExportDelegate;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.Folder;
import com.independentsoft.share.Group;
import com.independentsoft.share.User;

public abstract class ShptExportDelegate<T> extends
	ExportDelegate<T, ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportDelegateFactory, ShptExportEngine> {

	private static final Map<Class<?>, CmfObject.Archetype> TYPE_MAP;

	static {
		Map<Class<?>, CmfObject.Archetype> m = new LinkedHashMap<>();
		m.put(ShptVersion.class, CmfObject.Archetype.DOCUMENT);
		m.put(Folder.class, CmfObject.Archetype.FOLDER);
		m.put(Group.class, CmfObject.Archetype.GROUP);
		m.put(User.class, CmfObject.Archetype.USER);
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
	protected List<CmfContentStream> storeContent(ShptExportContext ctx, CmfAttributeTranslator<CmfValue> translator,
		CmfObject<CmfValue> marshalled, CmfContentStore<?, ?> streamStore, boolean includeRenditions) {
		return null;
	}

	@Override
	protected final CmfObject.Archetype calculateType(ShptSession session, T object) throws Exception {
		for (Map.Entry<Class<?>, CmfObject.Archetype> e : ShptExportDelegate.TYPE_MAP.entrySet()) {
			if (e.getKey().isInstance(object)) { return e.getValue(); }
		}
		return null;
	}
}