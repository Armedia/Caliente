/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

import java.io.File;
import java.util.Iterator;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.sharepoint.ShptException;
import com.armedia.caliente.engine.sharepoint.ShptSession;
import com.armedia.caliente.engine.sharepoint.ShptSessionException;
import com.armedia.caliente.engine.sharepoint.ShptSessionFactory;
import com.armedia.caliente.engine.sharepoint.ShptSessionWrapper;
import com.armedia.caliente.engine.sharepoint.ShptSetting;
import com.armedia.caliente.engine.sharepoint.ShptTranslator;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.StreamTools;

/**
 *
 *
 */
public class ShptExportEngine extends
	ExportEngine<ShptSession, ShptSessionWrapper, CmfValue, ShptExportContext, ShptExportContextFactory, ShptExportDelegateFactory, ShptExportEngineFactory> {

	public ShptExportEngine(ShptExportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, true, SearchType.PATH);
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByQuery(ShptSession session, CfgTools configuration,
		ShptExportDelegateFactory factory, String query) throws Exception {
		throw new Exception("SharePoint export doesn't yet support query-based export");
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsBySearchKey(ShptSession session, CfgTools configuration,
		ShptExportDelegateFactory factory, String searchKey) throws Exception {
		throw new Exception("SharePoint export doesn't yet support ID-based export");
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByPath(ShptSession service, CfgTools configuration,
		ShptExportDelegateFactory factory, String path) throws Exception {
		// support query by path (i.e. all files in these paths)
		// support query by Sharepoint query language
		if (service == null) {
			throw new IllegalArgumentException("Must provide a session through which to retrieve the results");
		}
		if (path == null) { throw new ShptException("Must provide the name of the site to export"); }
		final boolean excludeEmptyFolders = configuration.getBoolean(ShptSetting.EXCLUDE_EMPTY_FOLDERS);
		final Iterator<ExportTarget> it;
		try {
			it = new ShptRecursiveIterator(service, service.getFolder(path), configuration, excludeEmptyFolders);
		} catch (ShptSessionException e) {
			throw new ShptException(String.format("Export target search failed for path [%s]", path), e);
		}
		return StreamTools.of(it);
	}

	@Override
	protected CmfValue getValue(CmfValue.Type type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmfAttributeTranslator<CmfValue> getTranslator() {
		return ShptTranslator.INSTANCE;
	}

	@Override
	protected ShptSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new ShptSessionFactory(cfg, crypto);
	}

	@Override
	protected ShptExportContextFactory newContextFactory(ShptSession session, CfgTools cfg,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new ShptExportContextFactory(this, cfg, session, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected ShptExportDelegateFactory newDelegateFactory(ShptSession session, CfgTools cfg) throws Exception {
		return new ShptExportDelegateFactory(this, cfg);
	}
}