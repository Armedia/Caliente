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
package com.armedia.caliente.engine.local.exporter;

import java.nio.file.FileSystems;
import java.nio.file.attribute.UserPrincipalLookupService;

import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.local.common.LocalRoot;
import com.armedia.caliente.engine.local.common.LocalSessionWrapper;
import com.armedia.caliente.engine.local.common.LocalSetting;
import com.armedia.caliente.engine.tools.PathTools;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;

public class LocalExportDelegateFactory
	extends ExportDelegateFactory<LocalRoot, LocalSessionWrapper, CmfValue, LocalExportContext, LocalExportEngine> {

	private final boolean copyContent;
	private final UserPrincipalLookupService userDb;
	private final boolean minimalDiskAccess;

	protected LocalExportDelegateFactory(LocalExportEngine engine, CfgTools configuration) throws Exception {
		super(engine, configuration);
		this.copyContent = configuration.getBoolean(LocalSetting.COPY_CONTENT);
		this.userDb = FileSystems.getDefault().getUserPrincipalLookupService();
		this.minimalDiskAccess = configuration.getBoolean(LocalSetting.MINIMAL_DISK_ACCESS);
	}

	public final LocalRoot getRoot() {
		return this.engine.getRoot();
	}

	public final LocalFile getLocalFile(String p) throws Exception {
		return this.engine.getLocalFile(p);
	}

	public final boolean isCopyContent() {
		return this.copyContent;
	}

	public final boolean isMinimalDiskAccess() {
		return this.minimalDiskAccess;
	}

	public final void loadAttributes(LocalExportContext ctx, CmfObject<CmfValue> object) throws ExportException {
		this.engine.loadAttributes(ctx, object);
	}

	@Override
	protected LocalExportDelegate<?> newExportDelegate(LocalRoot session, ExportTarget target) throws Exception {
		CmfObject.Archetype type = target.getType();
		String searchKey = target.getSearchKey();
		switch (type) {
			case FOLDER:
			case DOCUMENT:
				return new LocalFileExportDelegate(this, session,
					this.engine.getLocalFile(LocalRoot.normalize(PathTools.decodeSafePath(searchKey))));
			case USER:
				return new LocalPrincipalExportDelegate(this, session, this.userDb.lookupPrincipalByName(searchKey));
			case GROUP:
				return new LocalPrincipalExportDelegate(this, session,
					this.userDb.lookupPrincipalByGroupName(searchKey));
			default:
				break;
		}
		this.log.warn("Type [{}] is not supported - no delegate created for search key [{}]", type, searchKey);
		return null;
	}
}