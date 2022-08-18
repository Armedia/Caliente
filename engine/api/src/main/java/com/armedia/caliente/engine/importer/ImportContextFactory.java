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
package com.armedia.caliente.engine.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.caliente.engine.TransferContextFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.common.SessionWrapper;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

public abstract class ImportContextFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, ?>, //
	ENGINE extends ImportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?, ?>, //
	FOLDER //
> extends TransferContextFactory<SESSION, VALUE, CONTEXT, ENGINE> {

	private final List<String> rootPath;
	private final String rootPathStr;
	private final int pathTrunc;

	protected ImportContextFactory(ENGINE engine, CfgTools settings, SESSION session, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> contentStore, Transformer transformer, Logger output, WarningTracker tracker)
		throws Exception {
		super(engine, settings, session, objectStore, contentStore, transformer, output, tracker);
		String rootPath = settings.getString(ImportSetting.TARGET_LOCATION);
		this.rootPath = Tools.freezeList(FileNameTools.tokenize(rootPath, '/'));
		this.pathTrunc = Math.max(0, settings.getInteger(ImportSetting.TRIM_PREFIX));
		if (this.rootPath.isEmpty()) {
			this.rootPathStr = "/";
		} else {
			this.rootPathStr = FileNameTools.reconstitute(this.rootPath, true, false, '/');
		}
	}

	final void ensureTargetPath(SESSION session) throws ImportException {
		try {
			ensurePath(session, this.rootPathStr);
		} catch (Exception e) {
			throw new ImportException(
				String.format("Failed to ensure the existence of the target path [%s]", this.rootPathStr), e);
		}
	}

	private FOLDER ensurePath(SESSION session, String path) throws Exception {
		if (Objects.equals("/", path)) { return null; }
		FOLDER target = locateFolder(session, path);
		if (target == null) {
			FOLDER parent = ensurePath(session, FileNameTools.dirname(path, '/'));
			target = createFolder(session, parent, FileNameTools.basename(path, '/'));
		}
		return target;
	}

	protected abstract FOLDER locateFolder(SESSION session, String path) throws Exception;

	protected abstract FOLDER createFolder(SESSION session, FOLDER parent, String name) throws Exception;

	protected static final String getTargetPath(String sourcePath, int pathTrunc, List<String> rootPath)
		throws ImportException {
		if (sourcePath == null) { throw new IllegalArgumentException("Must provide a path to transform"); }
		List<String> l = FileNameTools.tokenize(sourcePath, '/');
		if (l.size() < pathTrunc) {
			throw new ImportException(String.format(
				"The path truncation setting (%d) is higher than the number of path components in [%s] (%d) - can't continue",
				pathTrunc, sourcePath, l.size()));
		}
		l = l.subList(pathTrunc, l.size());
		List<String> finalPath = new ArrayList<>(rootPath.size() + l.size());
		finalPath.addAll(rootPath);
		finalPath.addAll(l);
		return FileNameTools.reconstitute(finalPath, true, false, '/');
	}

	public final String getTargetPath(String sourcePath) throws ImportException {
		return ImportContextFactory.getTargetPath(sourcePath, this.pathTrunc, this.rootPath);
	}

	public final boolean isPathAltering() {
		return (this.pathTrunc != 0) || !this.rootPath.isEmpty();
	}

	@Override
	protected void calculateExcludes(CmfObjectStore<?> objectStore, Set<CmfObject.Archetype> excludes)
		throws CmfStorageException {
		Map<CmfObject.Archetype, Long> summary = objectStore.getStoredObjectTypes();
		if ((summary != null) && !summary.isEmpty()) {
			for (CmfObject.Archetype t : CmfObject.Archetype.values()) {
				Long count = summary.get(t);
				// If the object type isn't even included (null or 0-count), then
				// we add the object to the excludes list to avoid problems.
				if ((count == null) || (count.longValue() < 1)) {
					excludes.add(t);
				}
			}
		} else {
			excludes = EnumSet.allOf(CmfObject.Archetype.class);
		}
	}

	public final Map<CmfObjectRef, String> getObjectNames(Collection<CmfObjectRef> refs, boolean current)
		throws ImportException {
		try {
			return getObjectStore().getObjectNames(refs, current);
		} catch (CmfStorageException e) {
			throw new ImportException(String.format("Failed to resolve the object names for IDs %s", refs), e);
		}
	}

	@Override
	protected String getContextLabel() {
		return "import";
	}
}