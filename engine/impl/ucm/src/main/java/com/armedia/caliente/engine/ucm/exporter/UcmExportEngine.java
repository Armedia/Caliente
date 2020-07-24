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
package com.armedia.caliente.engine.ucm.exporter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionFactory;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.engine.ucm.UcmTranslator;
import com.armedia.caliente.engine.ucm.model.UcmFSObject;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.caliente.engine.ucm.model.UcmFolderNotFoundException;
import com.armedia.caliente.engine.ucm.model.UcmModel;
import com.armedia.caliente.engine.ucm.model.UcmServiceException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class UcmExportEngine extends
	ExportEngine<UcmSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportContextFactory, UcmExportDelegateFactory, UcmExportEngineFactory> {

	private static final Pattern STATIC_PARSER = Pattern.compile("^(file|folder)\\s*:\\s*(.+)$",
		Pattern.CASE_INSENSITIVE);

	public UcmExportEngine(UcmExportEngineFactory factory, Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, true);
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByQuery(UcmSession session, CfgTools configuration, String query)
		throws Exception {
		if (StringUtils.isEmpty(query)) { return Stream.empty(); }

		// TODO: Not like!! Doesn't match the spirit of what we're trying to do
		Builder<ExportTarget> builder = Stream.builder();
		session.iterateURISearchResults(query,
			(s, o, u) -> builder.accept(new ExportTarget(CmfObject.Archetype.DOCUMENT, u.toString(), u.toString())));
		return builder.build();
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsBySearchKey(UcmSession session, CfgTools configuration, String line)
		throws Exception {
		// Remove leading and trailing space
		line = StringUtils.strip(line);

		Matcher m = UcmExportEngine.STATIC_PARSER.matcher(line);
		if (!m.matches()) {
			this.log.error("Malformed Search Key [{}]", line);
			return null;
		}

		String type = m.group(1);
		String key = m.group(2);

		URI uri = null;
		try {
			uri = new URI(type, key, null);
		} catch (URISyntaxException e) {
			this.log.error("Can't form a URI from the Search Key [{}] -> [{}, {}]", line, type, key);
			return null;
		}

		if (!UcmModel.isFolderURI(uri)) {
			return Stream.of(new ExportTarget(CmfObject.Archetype.DOCUMENT, uri.toString(), uri.toString()));
		}

		// This is a folder....
		try {
			UcmFolder folder = session.getFolder(uri);
			if (folder == null) { return null; }
			return findExportTargetsByPath(session, configuration, folder.getPath());
		} catch (UcmFolderNotFoundException e) {
			return null;
		}
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByPath(UcmSession session, CfgTools configuration, String path)
		throws Exception {
		UcmFSObject object = session.getObject(path);
		switch (object.getType()) {
			case FILE:
				return Stream.of(new ExportTarget(CmfObject.Archetype.DOCUMENT, object.getUniqueURI().toString(),
					object.getURI().toString()));

			case FOLDER:
				if (object.isShortcut()) {
					return Stream.of(new ExportTarget(CmfObject.Archetype.FOLDER, object.getUniqueURI().toString(),
						object.getURI().toString()));
				}

				// TODO: Not like!! Doesn't match the spirit of what we're trying to do
				Builder<ExportTarget> builder = Stream.builder();
				UcmFolder folder = UcmFolder.class.cast(object);
				// Not a shortcut, so we'll recurse into it and submit each and every one of
				// its contents, but we won't be recursing into shortcuts
				session.iterateFolderContentsRecursive(folder, false, (s, p, u, o) -> builder.accept(
					new ExportTarget(o.getType().archetype, o.getUniqueURI().toString(), o.getURI().toString())));
				return builder.build();
		}
		return Stream.empty();
	}

	@Override
	protected String findFolderName(UcmSession session, String folderId, Object ecmObject) {
		try {
			UcmFolder folder = session.getFolder(new URI(folderId));
			return (folder != null ? folder.getName() : null);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	protected CmfValue getValue(CmfValue.Type type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected UcmSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new UcmSessionFactory(cfg, crypto);
	}

	@Override
	protected UcmExportContextFactory newContextFactory(UcmSession session, CfgTools cfg, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> streamStore, Transformer transformer, Logger output, WarningTracker warningTracker)
		throws Exception {
		return new UcmExportContextFactory(this, session, cfg, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected UcmExportDelegateFactory newDelegateFactory(UcmSession session, CfgTools cfg) throws Exception {
		return new UcmExportDelegateFactory(this, cfg);
	}

	@Override
	protected UcmTranslator getTranslator() {
		return new UcmTranslator();
	}

	@Override
	protected void validateEngine(UcmSession session) throws ExportException {
		try {
			Boolean b = UcmModel.isFrameworkFoldersEnabled(session);
			if (b == null) {
				// Uknown if it's enabled...assume it's OK but log a warning
				this.log.warn(
					"Could not determine if FrameworkFolders is enabled because the user [{}] lacks the necessary permissions to perform the check. Will proceed as if it's enabled, but this may cause problems moving forward.",
					session.getUserContext().getUser());
			} else if (!b.booleanValue()) {
				throw new ExportException("FrameworkFolders is not enabled in this UCM server instance");
			}
		} catch (UcmServiceException e) {
			throw new ExportException("Failed to validate the UCM connectivity", e);
		}
	}

	public static List<String> decodePathList(String paths) {
		if (StringUtils.isEmpty(paths)) { return Collections.emptyList(); }
		List<String> ret = new ArrayList<>();
		for (String str : Tools.splitEscaped(',', paths)) {
			if (!StringUtils.isEmpty(str)) {
				ret.add(str);
			}
		}
		return ret;
	}

	public static String encodePathList(Collection<String> paths) {
		if ((paths == null) || paths.isEmpty()) { return ""; }
		StringBuilder sb = new StringBuilder();
		for (String s : paths) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			if (!StringUtils.isEmpty(s)) {
				sb.append(s.replaceAll(",", "\\\\,"));
			}
		}
		return sb.toString();
	}

	public static String encodePathList(String... paths) {
		if (paths == null) { return null; }
		return UcmExportEngine.encodePathList(Arrays.asList(paths));
	}
}