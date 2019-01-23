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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportResultSubmitter;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionFactory;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.engine.ucm.UcmTranslator;
import com.armedia.caliente.engine.ucm.model.UcmFSObject;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.caliente.engine.ucm.model.UcmModel;
import com.armedia.caliente.engine.ucm.model.UcmModel.ObjectHandler;
import com.armedia.caliente.engine.ucm.model.UcmModel.URIHandler;
import com.armedia.caliente.engine.ucm.model.UcmRuntimeException;
import com.armedia.caliente.engine.ucm.model.UcmServiceException;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class UcmExportEngine extends
	ExportEngine<UcmSession, UcmSessionWrapper, CmfValue, UcmExportContext, UcmExportContextFactory, UcmExportDelegateFactory, UcmExportEngineFactory> {

	private static final Pattern STATIC_PARSER = Pattern.compile("^(file|folder)\\s*:\\s*(.+)$",
		Pattern.CASE_INSENSITIVE);

	private static class ExceptionWrapper extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final URI uri;

		private ExceptionWrapper(URI uri, Throwable cause) {
			super(cause);
			this.uri = uri;
		}
	}

	public UcmExportEngine(UcmExportEngineFactory factory, Logger output, WarningTracker warningTracker, File baseData,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, true);
	}

	@Override
	protected void findExportTargetsByQuery(UcmSession session, CfgTools configuration,
		UcmExportDelegateFactory factory, ExportResultSubmitter handler, String query) throws Exception {
		if (StringUtils.isEmpty(query)) { return; }
		try {
			session.iterateURISearchResults(query, new URIHandler() {
				@Override
				public void handleURI(UcmSession session, long pos, URI objectUri) {
					try {
						handler.submit(new ExportTarget(CmfType.DOCUMENT, objectUri.toString(), objectUri.toString()));
					} catch (final ExportException e) {
						throw new ExceptionWrapper(objectUri, e);
					}
				}
			});
		} catch (ExceptionWrapper e) {
			throw new ExportException(String.format("Exception caught while handling search result [%s]", e.uri),
				e.getCause());
		}
	}

	@Override
	protected ExportTarget findExportTarget(UcmSession session, String line) throws Exception {
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

		CmfType cmfType = null;
		if (type.equalsIgnoreCase("file")) {
			cmfType = CmfType.DOCUMENT;
		} else {
			cmfType = CmfType.FOLDER;
		}

		return new ExportTarget(cmfType, uri.toString(), uri.toString());
	}

	@Override
	protected void findExportTargetsByPath(UcmSession session, CfgTools configuration, UcmExportDelegateFactory factory,
		ExportResultSubmitter handler, String path) throws Exception {
		UcmFSObject object = session.getObject(path);
		switch (object.getType()) {
			case FILE:
				handler.submit(
					new ExportTarget(CmfType.DOCUMENT, object.getUniqueURI().toString(), object.getURI().toString()));
				break;
			case FOLDER:
				if (object.isShortcut()) {
					handler.submit(
						new ExportTarget(CmfType.FOLDER, object.getUniqueURI().toString(), object.getURI().toString()));
					break;
				}
				UcmFolder folder = UcmFolder.class.cast(object);
				// Not a shortcut, so we'll recurse into it and submit each and every one of
				// its contents, but we won't be recursing into shortcuts
				session.iterateFolderContentsRecursive(folder, false, new ObjectHandler() {
					@Override
					public void handleObject(UcmSession session, long pos, URI objectUri, UcmFSObject object) {
						try {
							handler.submit(new ExportTarget(object.getType().cmfType, object.getUniqueURI().toString(),
								object.getURI().toString()));
						} catch (ExportException e) {
							throw new UcmRuntimeException(String.format(
								"ExportException caught while submitting item [%s] to the workload", objectUri), e);
						}
					}
				});
				break;
		}
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected UcmSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new UcmSessionFactory(cfg, crypto);
	}

	@Override
	protected UcmExportContextFactory newContextFactory(UcmSession session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
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
		for (String str : Tools.splitCSVEscaped(paths)) {
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