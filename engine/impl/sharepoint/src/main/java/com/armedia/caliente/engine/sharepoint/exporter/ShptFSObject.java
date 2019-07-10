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
/**
 *
 */

package com.armedia.caliente.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.sharepoint.ShptAttributes;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

/**
 *
 *
 */
public abstract class ShptFSObject<T> extends ShptObject<T> {

	private final String url;

	protected ShptFSObject(ShptExportDelegateFactory factory, ShptSession session, Class<T> objectClass, T object)
		throws Exception {
		super(factory, session, objectClass, object);
		this.url = calculateServerRelativeUrl(session, object);
	}

	@Override
	protected String calculateObjectId(ShptSession session, T object) {
		String searchKey = calculateServerRelativeUrl(session, object);
		return String.format("%08X", Tools.hashTool(searchKey, null, searchKey));
	}

	@Override
	public String calculateHistoryId(ShptSession session, T object) {
		return calculateObjectId(session, object);
	}

	@Override
	protected String calculateSearchKey(ShptSession session, T object) {
		return calculateServerRelativeUrl(session, object);
	}

	protected abstract String calculateServerRelativeUrl(ShptSession session, T object);

	public final String getServerRelativeUrl() {
		return this.url;
	}

	public abstract Date getCreatedTime();

	public abstract Date getLastModifiedTime();

	@Override
	protected boolean marshal(ShptExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		// Name
		String name = getName();
		final boolean root = StringUtils.isEmpty(name);
		if (root) {
			name = getServerRelativeUrl();
			name = FileNameTools.removeEdgeSeparators(name, '/');
			name = name.replaceFirst("/", "_");
		}
		object.setAttribute(new CmfAttribute<>(ShptAttributes.OBJECT_NAME.name, CmfValue.Type.STRING, false,
			Collections.singleton(new CmfValue(name))));

		Date d = getCreatedTime();
		if (d != null) {
			object.setAttribute(new CmfAttribute<>(ShptAttributes.CREATE_DATE.name, CmfValue.Type.DATETIME, false,
				Collections.singleton(new CmfValue(d))));
		}

		d = getLastModifiedTime();
		if (d != null) {
			object.setAttribute(new CmfAttribute<>(ShptAttributes.MODIFICATION_DATE.name, CmfValue.Type.DATETIME, false,
				Collections.singleton(new CmfValue(d))));
		}

		// Target Paths
		if (!root) {
			// TODO: is this safe? What if we have a "3-level root"? i.e. /sites/blabla/root
			String path = getServerRelativeUrl();
			path = FileNameTools.dirname(path, '/');
			path = FileNameTools.removeEdgeSeparators(path, '/').replaceFirst("/", "_");
			path = String.format("/%s", path);
			if (this.log.isDebugEnabled()) {
				this.log.debug("Setting target path [{}] from source path [{}] for {} [ID={}/L={}]", path,
					getServerRelativeUrl(), getType(), getObjectId(), getLabel());
			}
			object.setProperty(new CmfProperty<>(IntermediateProperty.PATH, CmfValue.Type.STRING, true,
				Collections.singleton(new CmfValue(path))));
		}
		return true;
	}

	@Override
	protected Collection<ShptObject<?>> findRequirements(ShptSession session, CmfObject<CmfValue> marshaled,
		ShptExportContext ctx) throws Exception {
		Collection<ShptObject<?>> ret = super.findRequirements(session, marshaled, ctx);
		if (!StringUtils.isEmpty(getName())) {
			String parentPath = getServerRelativeUrl();
			parentPath = FileNameTools.dirname(parentPath, '/');
			ShptFolder parent = new ShptFolder(this.factory, session, session.getFolder(parentPath));
			marshaled.setProperty(new CmfProperty<>(IntermediateProperty.PARENT_ID, CmfValue.Type.ID, true,
				Collections.singleton(new CmfValue(CmfValue.Type.ID, parent.getObjectId()))));
			ret.add(parent);
			if (this.log.isDebugEnabled()) {
				this.log.debug("Adding parent dependency to [{}] from source path [{}] for {} [ID={}/L={}]", parentPath,
					getServerRelativeUrl(), getType(), getObjectId(), getLabel());
			}
		}
		return ret;
	}
}