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
package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.caliente.engine.tools.KeyLockableCache.ReferenceType;
import com.armedia.caliente.engine.ucm.UcmConstants;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.function.CheckedTriConsumer;

import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet;
import oracle.stellent.ridc.protocol.ServiceException;
import oracle.stellent.ridc.protocol.ServiceResponse;
import oracle.stellent.ridc.protocol.ServiceResponse.ResponseType;

// FLD_CREATE_FILE (fParentGUID, dDocName, fRelationshipType="(owner|soft)")
// FLD_CREATE_FILE (fParentGUID, fFileGUID, fRelationshipType="(owner|soft)") (???)
// FLD_CREATE_FOLDER (fParentGUID, fFolderGUID, fRelationshipType="(owner|soft)")
// CHECKIN_UNIVERSAL (dDocAuthor, dDocTitle, dDocType, dSecurityGroup, dCreateDate, doFileCopy = 1)
public class UcmModel {
	// Syntax: query{sortAtts}[startRow,rowCount] / sortAtts == +att1,-att2,
	private static final Pattern QUERY_PARSER = Pattern
		.compile("^(.*?)(?:\\s*\\{\\s*(.*)\\s*\\}\\s*)?(?:\\s*\\[\\s*(.*)\\s*\\]\\s*)?$", Pattern.DOTALL);
	private static final Pattern SORT_PARSER = Pattern.compile("^[-+]?\\w+$");
	private static final Pattern ROW_PARSER = Pattern
		.compile("^(?:\\s*([1-9][0-9]*)\\s*,)?\\s*([1-9][0-9]*)\\s*(?:/\\s*([1-9][0-9]*))?\\s*$");
	private static final Pattern PATH_CHECKER = Pattern.compile("^(/|(/[^/]+)+/?)$");
	private static final String RENDITION_DEFAULT_TYPE = UcmRenditionInfo.DEFAULT;
	private static final String REMDITION_DEFAULT_FORMAT = "application/octet-stream";
	private static final int MIN_OBJECT_COUNT = 100;
	private static final int DEFAULT_OBJECT_COUNT = 10000;
	private static final int MAX_OBJECT_COUNT = 1000000;

	private static final String FILE_SCHEME = "file";
	private static final String FILELINK_SCHEME = "filelink";
	private static final String FOLDER_SCHEME = "folder";
	private static final String NULL_SCHEME = "null";

	static final URI NULL_URI = UcmModel.newURI(UcmModel.NULL_SCHEME, "null");
	static final String NULL_FOLDER_GUID = "idcnull";
	static final URI NULL_FOLDER_URI = UcmModel.newFolderURI(UcmModel.NULL_FOLDER_GUID);

	static final URI ROOT_URI = UcmModel.newFolderURI("FLD_ROOT");

	private static class UcmServiceResponse {
		private final UcmAttributes attributes;
		private final DataResultSet history;
		private final DataResultSet renditions;

		private UcmServiceResponse(UcmAttributes attributes) {
			this(attributes, null, null);
		}

		private UcmServiceResponse(UcmAttributes attributes, DataResultSet history) {
			this(attributes, history, null);
		}

		private UcmServiceResponse(UcmAttributes attributes, DataResultSet history, DataResultSet renditions) {
			this.attributes = Objects.requireNonNull(attributes, "Must provide the attribute response");
			this.history = history;
			this.renditions = renditions;
		}

		public UcmAttributes getAttributes() {
			return this.attributes;
		}

		public DataResultSet getHistory() {
			return this.history;
		}

		public DataResultSet getRenditions() {
			return this.renditions;
		}
	}

	// UniqueURI:
	// * FILE -> file:${dDocName}#${dID}
	// * FOLDER -> folder:${fFolderGUID}

	// UniqueURI -> UcmFSObject
	private final KeyLockableCache<UcmUniqueURI, UcmFSObject> objectByUniqueURI;

	// UcmUniqueURI -> Map<String, UcmRenditionInfo>
	private final KeyLockableCache<UcmUniqueURI, Map<String, UcmRenditionInfo>> renditionsByUniqueURI;

	// UniqueURI -> UcmFSObject
	private final KeyLockableCache<URI, UcmFSObject> objectByHistoryURI;

	// HistoryURI -> List<UcmRevision>
	private final KeyLockableCache<URI, List<UcmRevision>> historyByURI;

	// path -> HistoryURI
	private final KeyLockableCache<String, URI> uriByPaths;

	// Child HistoryURI -> Parent HistoryURI
	private final KeyLockableCache<URI, URI> parentByURI;

	// Parent HistoryURI -> Map<Child Name, Child HistoryURI>
	private final KeyLockableCache<URI, Map<String, URI>> childrenByURI;

	// dID -> UcmUniqueURI
	private final KeyLockableCache<String, UcmUniqueURI> revisionUriByRevisionID;

	// UniqueURI -> HistoryURI
	private final KeyLockableCache<UcmUniqueURI, URI> historyUriByUniqueURI;

	private final Map<Integer, String> cacheNames;

	public static DataBinder getConfigInfo(UcmSession s) throws UcmServiceException {
		try {
			ServiceResponse response = s.callService("CONFIG_INFO");
			return response.getResponseAsBinder();
		} catch (final IdcClientException e) {
			if (ServiceException.class.isInstance(e)) {
				ServiceException se = ServiceException.class.cast(e);

				DataBinder binder = se.getBinder();
				DataObject local = binder.getLocalData();

				int statusCode = local.getInteger("StatusCode");
				if (statusCode == -1) {
					String mk = local.get("StatusMessageKey");
					List<UcmExceptionData.Entry> entries = UcmExceptionData.parseMessageKey(mk);
					if (entries.size() == 2) {
						UcmExceptionData.Entry first = entries.get(0);
						UcmExceptionData.Entry second = entries.get(1);
						if (first.tagIs("csUnableToRetrieveConfigInfo") && second.tagIs("csUserInsufficientAccess")) {
							// This user lacks access to CONFIG_INFO, so just return null
							// instead of an error, since this allows the caller to decide what
							// to do about this
							return null;
						}
					}
				}
			}

			throw new UcmServiceException("Failed to retrieve the system configuration information using CONFIG_INFO",
				e);
		}
	}

	public static Boolean isFrameworkFoldersEnabled(UcmSession s) throws UcmServiceException {
		DataBinder responseData = UcmModel.getConfigInfo(s);
		if (responseData == null) { return null; }

		// First things first!! Stash the retrieved object...
		DataResultSet rs = responseData.getResultSet("EnabledComponents");
		List<DataObject> components = rs.getRows();
		for (DataObject component : components) {
			if (("FrameworkFolders".equals(component.get("name"))) && ("Enabled".equals(component.get("status")))) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	public static boolean isRoot(URI uri) {
		return UcmModel.ROOT_URI.equals(uri);
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	public UcmModel() throws UcmServiceException {
		this(UcmModel.DEFAULT_OBJECT_COUNT);
	}

	public UcmModel(int objectCount) throws UcmServiceException {
		objectCount = Tools.ensureBetween(UcmModel.MIN_OBJECT_COUNT, objectCount, UcmModel.MAX_OBJECT_COUNT);

		Map<Integer, String> cacheNames = new LinkedHashMap<>();

		// We don't want things to expire - that's what LRU caching is for
		final Duration d = Duration.ofSeconds(-1);
		final ReferenceType t = ReferenceType.FINAL;
		this.objectByUniqueURI = new KeyLockableCache<>(t, objectCount, d);
		cacheNames.put(System.identityHashCode(this.objectByUniqueURI), "objectByUniqueURI");
		this.renditionsByUniqueURI = new KeyLockableCache<>(t, objectCount, d);
		cacheNames.put(System.identityHashCode(this.renditionsByUniqueURI), "renditionsByUniqueURI");
		this.objectByHistoryURI = new KeyLockableCache<>(t, objectCount, d);
		cacheNames.put(System.identityHashCode(this.objectByHistoryURI), "objectByHistoryURI");
		this.historyByURI = new KeyLockableCache<>(t, objectCount, d);
		cacheNames.put(System.identityHashCode(this.historyByURI), "historyByURI");

		this.uriByPaths = new KeyLockableCache<>(t, objectCount, d);
		cacheNames.put(System.identityHashCode(this.uriByPaths), "uriByPaths");
		this.parentByURI = new KeyLockableCache<>(t, objectCount, d);
		cacheNames.put(System.identityHashCode(this.parentByURI), "parentByURI");
		this.childrenByURI = new KeyLockableCache<>(t, objectCount, d);
		cacheNames.put(System.identityHashCode(this.childrenByURI), "childrenByURI");
		this.historyUriByUniqueURI = new KeyLockableCache<>(t, objectCount, d);
		cacheNames.put(System.identityHashCode(this.historyUriByUniqueURI), "historyUriByUniqueURI");
		this.revisionUriByRevisionID = new KeyLockableCache<>(t, objectCount, d);
		cacheNames.put(System.identityHashCode(this.revisionUriByRevisionID), "revisionUriByRevisionID");
		this.cacheNames = Tools.freezeMap(cacheNames);
	}

	private boolean canCache(KeyLockableCache<?, ?> locked, KeyLockableCache<?, ?> cache) {
		final String name = this.cacheNames.getOrDefault(System.identityHashCode(cache),
			String.valueOf(System.identityHashCode(cache)));
		if (locked == cache) {
			this.log.trace("Avoiding recursive cache locking: {}", name);
			return false;
		}
		return true;
	}

	private <K extends Serializable, V> V putInCache(KeyLockableCache<?, ?> locked, KeyLockableCache<K, V> cache, K k,
		V v) {
		if (!canCache(locked, cache)) { return null; }
		final String name = this.cacheNames.getOrDefault(System.identityHashCode(cache),
			String.valueOf(System.identityHashCode(cache)));
		this.log.trace("Caching into {}: {} = {}", name, k, v);
		return cache.put(k, v);
	}

	protected UcmFSObject cacheObject(final UcmFSObject object) {
		return cacheObject(null, object, null, null);
	}

	protected UcmFSObject cacheObject(KeyLockableCache<?, ?> cache, final UcmServiceResponse rsp) {
		return cacheObject(cache, newFSObject(rsp.getAttributes()), rsp::getHistory, rsp::getRenditions);
	}

	protected UcmFSObject cacheObject(KeyLockableCache<?, ?> cache, final UcmFSObject object) {
		return cacheObject(cache, object, null, null);
	}

	protected UcmFSObject cacheObject(KeyLockableCache<?, ?> cache, final UcmFSObject object,
		Supplier<DataResultSet> history, Supplier<DataResultSet> renditions) {
		if (object == null) {
			this.log.trace("cacheObject() -> nothing to cache");
			return null;
		}
		// Is this a file or a folder?
		final URI historyUri = object.getURI();
		final UcmUniqueURI uniqueUri = object.getUniqueURI();

		// First, the primary objects
		putInCache(cache, this.objectByUniqueURI, uniqueUri, object);

		// We only save to cache if it's not a file, or it's the latest revision
		UcmFile file = Tools.cast(UcmFile.class, object);
		if ((file == null) || file.isLatestRevision()) {
			putInCache(cache, this.objectByHistoryURI, historyUri, object);
		}

		if ((history != null) && canCache(cache, this.historyByURI)) {
			DataResultSet rs = history.get();
			if (rs != null) {
				LinkedList<UcmRevision> list = new LinkedList<>();
				for (DataObject o : rs.getRows()) {
					list.addFirst(new UcmRevision(historyUri, o, rs.getFields()));
				}
				putInCache(cache, this.historyByURI, historyUri, Tools.freezeList(new ArrayList<>(list)));
			}
		}

		if ((renditions != null) && canCache(cache, this.renditionsByUniqueURI)) {
			DataResultSet rs = renditions.get();
			if (rs != null) {
				Map<String, UcmRenditionInfo> m = new TreeMap<>();
				for (DataObject o : rs.getRows()) {
					UcmRenditionInfo r = new UcmRenditionInfo(uniqueUri, o, rs.getFields());
					m.put(r.getType().toUpperCase(), r);
				}
				putInCache(cache, this.renditionsByUniqueURI, uniqueUri, Tools.freezeMap(new LinkedHashMap<>(m)));
			}
		}

		// Now, the pointers...
		if (object.hasAttribute(UcmAtt.fParentGUID)) {
			putInCache(cache, this.parentByURI, historyUri,
				UcmModel.newFolderURI(object.getString(UcmAtt.fParentGUID)));
		}

		putInCache(cache, this.historyUriByUniqueURI, uniqueUri, historyUri);

		return object;
	}

	protected static final URI getURI(UcmAttributes data) {
		// Folders are handled first - easiest case
		if (data.hasAttribute(UcmAtt.fFolderGUID)) { return UcmModel.newFolderURI(data.getString(UcmAtt.fFolderGUID)); }

		// Next are file shortcuts, because we need to handle them differently...
		final String fTargetGUID = data.getString(UcmAtt.fTargetGUID);
		if (!StringUtils.isBlank(fTargetGUID)) { return UcmModel.newFileLinkURI(fTargetGUID); }

		// Finally, regular files
		if (data.hasAttribute(UcmAtt.dDocName)) { return UcmModel.newFileURI(data.getString(UcmAtt.dDocName)); }

		// And, of course, when we don't know what to do...
		throw new UcmRuntimeException(String
			.format("Could not find either fFolderGUID, dDocName or fTargetGUID in the given attribute set: %s", data));
	}

	protected static final UcmUniqueURI getUniqueURI(UcmAttributes data) {
		if (data == null) { return null; }
		URI uri = UcmModel.getURI(data);
		if (uri == null) { return null; }
		if (UcmModel.isShortcut(data)) { return new UcmUniqueURI(uri); }
		if (UcmModel.isFileURI(uri)) {
			final String dID = data.getString(UcmAtt.dID);
			try {
				uri = new URI(uri.getScheme(), uri.getSchemeSpecificPart(), dID);
			} catch (URISyntaxException e) {
				throw new UcmRuntimeException(
					String.format("Failed to construct a file URI from [%s] and dID=[%s]", uri, dID), e);
			}
		}
		return new UcmUniqueURI(uri);
	}

	private static final URI newURI(String scheme, String ssp) {
		return UcmModel.newURI(scheme, ssp, null);
	}

	protected static final URI newFileURI(String ssp) {
		if (StringUtils.isEmpty(ssp)) { return null; }
		return UcmModel.newURI(UcmModel.FILE_SCHEME, ssp, null);
	}

	protected static final URI newFileLinkURI(String ssp) {
		if (StringUtils.isEmpty(ssp)) { return null; }
		return UcmModel.newURI(UcmModel.FILELINK_SCHEME, ssp, "0");
	}

	protected static final URI newFolderURI(String ssp) {
		if (StringUtils.isEmpty(ssp)) { return null; }
		return UcmModel.newURI(UcmModel.FOLDER_SCHEME, ssp, null);
	}

	protected static final URI newURI(String scheme, String ssp, String fragment) {
		if (StringUtils.isEmpty(scheme)) { throw new IllegalArgumentException("The URI scheme may not be empty"); }
		if (StringUtils.isEmpty(ssp)) {
			throw new IllegalArgumentException("The URI scheme-specific part may not be empty");
		}
		try {
			return new URI(scheme, ssp, fragment);
		} catch (URISyntaxException e) {
			throw new RuntimeException(
				String.format("Failed to construct a URI using ([%s], [%s], [%s])", scheme, ssp, fragment), e);
		}
	}

	protected final boolean isNotFoundException(Throwable e, String fmt, Object... args) throws UcmServiceException {
		if (e == null) { return false; }

		// Is this a service exception from which we can identify that the
		// item doesn't exist?
		if (!ServiceException.class.isInstance(e)) {
			// No, this isn't an exception we can analyze...
			throw new UcmServiceException(String.format(fmt, args), e);
		}

		// This may be an analyzable exception
		ServiceException se = ServiceException.class.cast(e);
		DataBinder binder = se.getBinder();
		DataObject local = binder.getLocalData();

		int statusCode = local.getInteger("StatusCode");
		// We know this to be access denied...
		/*
		 known error codes:
		 0 : success
		-1 : generic error / unknown error condition
		-2 : comm address not found
		-3 : comm failed connect
		-4 : comm failed send
		-5 : comm failed receive
		-5 : comm timeout received
		-16 : resource not found
		-17 : resource exists
		-18 : resource cannot access
		-19 : resource read only
		-20 : insufficient privileges
		-21 : failed login
		-22 : resource locked
		-23 : resource wrong version
		-24 : resource wrong type
		-25 : resource unavailable
		-26 : resource misconfigured
		-27 : resource not defined
		-32 : process error
		-33 : process unnecessary
		-34 : process logic error
		-48 : unhanded exception
		-49 : insufficient memory
		-50 : mismatched parameters
		-64 : activity aborted
		-65 : activity cancelled
		-66 : activity suspended
		-67 : activity warning abort
		 */
		switch (statusCode) {
			case -18:
			case -20: // Known to be access errors
				return false;

			case -16: // Known to be "not found" errors
				return true;

			default:
				break;
		}

		String mk = local.get("StatusMessageKey");
		List<UcmExceptionData.Entry> entries = UcmExceptionData.parseMessageKey(mk);
		Set<String> tags = new HashSet<>();
		for (UcmExceptionData.Entry entry : entries) {
			tags.add(entry.getTag());
		}

		// Known to be an access issue
		if (tags.contains("csUserInsufficientAccess")) { return false; }

		for (UcmExceptionData.Entry entry : entries) {
			String op = entry.getTag();
			if (Objects.equals(op, "csFldDoesNotExist") || //
				Objects.equals(op, "csUnableToGetRevInfo2") || //
				Objects.equals(op, "csGetFileUnableToFindRevision")) {
				// TODO: Maybe we have to index more error labels here?
				return true;
			}
		}
		return false;
	}

	public static final boolean isFileURI(URI uri) {
		Objects.requireNonNull(uri, "Must provide a non-null URI to check");
		final String scheme = uri.getScheme();
		return UcmModel.FILE_SCHEME.equals(scheme) || UcmModel.FILELINK_SCHEME.equals(scheme);
	}

	public static final boolean isFileLinkURI(URI uri) {
		Objects.requireNonNull(uri, "Must provide a non-null URI to check");
		return UcmModel.FILELINK_SCHEME.equals(uri.getScheme());
	}

	public static final boolean isFolderURI(URI uri) {
		Objects.requireNonNull(uri, "Must provide a non-null URI to check");
		return UcmModel.FOLDER_SCHEME.equals(uri.getScheme());
	}

	public static final boolean isShortcut(UcmAttributes att) {
		Objects.requireNonNull(att, "Must provide a non-null attribute set to check");
		return !StringUtils.isEmpty(att.getString(UcmAtt.fTargetGUID));
	}

	private UcmServiceResponse buildAttributesFromDocInfo(DataBinder responseData) throws UcmServiceException {
		Map<String, String> baseObj = new HashMap<>();
		// First things first!! Stash the retrieved object...
		DataResultSet docInfo = responseData.getResultSet("DOC_INFO");
		if (docInfo == null) { return null; }

		DataResultSet fileInfo = responseData.getResultSet("FileInfo");
		if (fileInfo != null) {
			baseObj.putAll(fileInfo.getRows().get(0));
		}

		String parentPath = responseData.getLocalData().get("fParentPath");
		if (!StringUtils.isEmpty(parentPath)) {
			// Capture the parent path...from DOC_INFO, it's stored in LocalData.fParentPath
			baseObj.put(UcmAtt.cmfParentPath.name(), responseData.getLocalData().get("fParentPath"));
		}

		baseObj.putAll(docInfo.getRows().get(0));

		DataResultSet history = responseData.getResultSet("REVISION_HISTORY");
		DataResultSet renditions = responseData.getResultSet("Renditions");

		UcmAttributes baseData = new UcmAttributes(baseObj, docInfo.getFields(),
			(fileInfo != null ? fileInfo.getFields() : null));
		return new UcmServiceResponse(baseData, history, renditions);
	}

	private UcmServiceResponse buildAttributesFromFldInfo(DataBinder responseData) {
		boolean file = true;
		DataResultSet rs = responseData.getResultSet("FileInfo");
		if (rs == null) {
			file = false;
			rs = responseData.getResultSet("FolderInfo");
		}
		if (rs == null) { return null; }

		Map<String, String> baseObj = new HashMap<>();
		baseObj.putAll(rs.getRows().get(0));
		// Capture the parent path - it's either LocalData.filePath or LocalData.folderPath...but it
		// also contains the filename so we need to dirname it
		String parentPath = responseData.getLocal(String.format("%sPath", file ? "file" : "folder"));
		if (!StringUtils.isEmpty(parentPath)) {
			baseObj.put(UcmAtt.cmfParentPath.name(), FileNameTools.dirname(parentPath, '/'));
		}
		return new UcmServiceResponse(new UcmAttributes(baseObj, rs.getFields()));
	}

	public UcmFSObject getObject(UcmSession s, String path) throws UcmServiceException, UcmObjectNotFoundException {
		final URI uri = resolvePath(s, path);
		return getFSObject(s, uri);
	}

	public UcmFile getFile(UcmSession s, String path) throws UcmServiceException, UcmFileNotFoundException {
		URI uri = null;
		try {
			uri = resolvePath(s, path);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}

		return getFile(s, uri);
	}

	public UcmFile getFile(UcmSession s, URI uri) throws UcmServiceException, UcmFileNotFoundException {
		// Ensure the target is a file...
		if (!UcmModel.isFileURI(uri)) {
			throw new UcmFileNotFoundException(String.format("The object with URI [%s] is not a file", uri));
		}

		try {
			UcmFSObject obj = getFSObject(s, uri);
			if (!UcmFile.class.isInstance(obj)) {
				throw new UcmFileNotFoundException(String.format("The file with URI [%s] does not exist", uri));
			}
			return UcmFile.class.cast(obj);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}
	}

	public UcmFile getFileByGUID(UcmSession s, String guid) throws UcmServiceException, UcmFileNotFoundException {
		try {
			final URI uri = UcmModel.newFileURI(guid);
			final UcmFSObject obj = getFSObject(s, uri);
			if (!UcmFile.class.isInstance(obj)) {
				throw new UcmFileNotFoundException(String.format("The file with URI [%s] does not exist", uri));
			}
			return UcmFile.class.cast(obj);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}
	}

	public UcmFolder getFolderByGUID(UcmSession s, String guid) throws UcmServiceException, UcmFolderNotFoundException {
		try {
			final URI uri = UcmModel.newFolderURI(guid);
			final UcmFSObject obj = getFSObject(s, uri);
			if (!UcmFolder.class.isInstance(obj)) {
				throw new UcmFileNotFoundException(String.format("The folder with URI [%s] does not exist", uri));
			}
			return UcmFolder.class.cast(obj);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	protected UcmFSObject newFSObject(URI uri, UcmAttributes att) {
		return UcmModel.isFileURI(uri) ? new UcmFile(this, uri, att) : new UcmFolder(this, uri, att);
	}

	protected UcmFSObject newFSObject(UcmAttributes att) {
		return newFSObject(UcmModel.getURI(att), att);
	}

	protected URI resolvePath(final UcmSession s, String p) throws UcmServiceException, UcmObjectNotFoundException {
		final String sanitizedPath = UcmModel.sanitizePath(p);
		try {
			return this.uriByPaths.computeIfAbsent(sanitizedPath, (path) -> {
				ServiceResponse response = null;
				DataBinder responseData = null;
				try {
					response = s.callService("FLD_INFO", (binder) -> binder.putLocal("path", path));
					responseData = response.getResponseAsBinder();
				} catch (final IdcClientException e) {
					if (isNotFoundException(e, "Exception caught retrieving the file at [%s]", path)) {
						throw new UcmObjectNotFoundException(String.format("No object found at path [%s]", path), e);
					}
					// This is a "regular" exception that we simply re-raise
					throw e;
				}

				final UcmServiceResponse rsp = buildAttributesFromFldInfo(responseData);
				final UcmAttributes attributes = rsp.getAttributes();
				if (attributes == null) {
					throw new UcmServiceException(String
						.format("Path [%s] was found via FLD_INFO, but was neither a file nor a folder?!?", path));
				}
				// There's an object...so stash it
				UcmFSObject object = cacheObject(this.uriByPaths, rsp);
				return object.getURI();
			});
		} catch (Exception e) {
			UcmModel.throwIfMatches(UcmServiceException.class, e);
			UcmModel.throwIfMatches(UcmObjectNotFoundException.class, e);
			throw new UcmServiceException(String.format("Exception caught searching for path [%s]", sanitizedPath), e);
		}
	}

	protected UcmFSObject getFSObject(final UcmSession s, final URI uri)
		throws UcmServiceException, UcmObjectNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to retrieve");
		// Take a quick shortcut to avoid unnecessary calls
		if (UcmModel.NULL_FOLDER_GUID.equals(uri.getSchemeSpecificPart())) { return null; }

		final boolean file;
		final boolean link;
		if (UcmModel.isFileURI(uri)) {
			// The SSP is the dDocName
			file = true;
			link = UcmModel.isFileLinkURI(uri);
		} else if (UcmModel.isFolderURI(uri)) {
			// The SSP is the fFolderGUID
			file = false;
			link = false;
		} else {
			// WTF?? Invalid URI
			throw new IllegalArgumentException(String.format("The URI [%s] doesn't point to a valid object", uri));
		}

		try {
			return this.objectByHistoryURI.computeIfAbsent(uri, (historyUri) -> {
				ServiceResponse response = null;
				DataBinder responseData = null;
				final UcmAtt identifierAtt;
				final String serviceName;
				final String searchKey;
				if (file && !link) {
					String id = historyUri.getFragment();
					if (id != null) {
						serviceName = "DOC_INFO";
						identifierAtt = UcmAtt.dID;
						searchKey = id;
					} else {
						serviceName = "DOC_INFO_BY_NAME";
						identifierAtt = UcmAtt.dDocName;
						searchKey = historyUri.getSchemeSpecificPart();
					}
				} else {
					identifierAtt = (file ? UcmAtt.fFileGUID : UcmAtt.fFolderGUID);
					serviceName = "FLD_INFO";
					searchKey = historyUri.getSchemeSpecificPart();
				}

				try {
					response = s.callService(serviceName, (binder) -> {
						binder.putLocal(identifierAtt.name(), searchKey);
						if (file) {
							binder.putLocal("isAddFolderMetadata", "1");
							binder.putLocal("includeFileRenditionsInfo", "1");
						}
					});
					responseData = response.getResponseAsBinder();
				} catch (final IdcClientException e) {
					if (isNotFoundException(e, "Exception caught locating the object using URI [%s]", historyUri)) {
						throw new UcmObjectNotFoundException(String.format("No object found with URI [%s]", uri), e);
					}
					// This is a "regular" exception that we simply re-raise
					throw e;
				}

				final UcmServiceResponse rsp;
				final UcmAttributes attributes;
				if (file && !link) {
					rsp = buildAttributesFromDocInfo(responseData);
				} else {
					rsp = buildAttributesFromFldInfo(responseData);
				}
				attributes = rsp.getAttributes();
				if (attributes == null) {
					throw new UcmServiceException(
						String.format("The URI [%s] found via %s(%s=%s) didn't contain any data?!?", historyUri,
							serviceName, identifierAtt.name(), searchKey));
				}
				return cacheObject(this.objectByHistoryURI, rsp);
			});
		} catch (Exception e) {
			UcmModel.throwIfMatches(UcmServiceException.class, e);
			UcmModel.throwIfMatches(UcmObjectNotFoundException.class, e);
			throw new UcmServiceException(String.format("Exception caught resolving URI [%s]", uri), e);
		}
	}

	public UcmFile getFileRevision(UcmSession s, UcmRevision revision)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileRevision(s, revision.getId());
	}

	public UcmFile getFileRevision(UcmSession s, UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileRevision(s, file.getRevisionId());
	}

	protected UcmFile getFileRevision(final UcmSession s, final String id)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {

		final UcmUniqueURI guid;

		try {
			this.log.trace("getFileRevision({})", id);
			guid = this.revisionUriByRevisionID.computeIfAbsent(id, (wantedId) -> {
				ServiceResponse response = null;
				DataBinder responseData = null;
				try {
					response = s.callService("DOC_INFO", (binder) -> {
						binder.putLocal("dID", wantedId);
						binder.putLocal("includeFileRenditionsInfo", "1");
						binder.putLocal("isAddFolderMetadata", "1");
					});
					responseData = response.getResponseAsBinder();
				} catch (final IdcClientException e) {
					if (isNotFoundException(e, "Exception caught retrieving the revision with ID [%s]", wantedId)) {
						throw new UcmFileRevisionNotFoundException(
							String.format("No revision found with ID [%s]", wantedId), e);
					}
					// This is a "regular" exception that we simply re-raise
					throw e;
				}

				final UcmServiceResponse rsp = buildAttributesFromDocInfo(responseData);
				UcmAttributes attributes = rsp.getAttributes();
				if (attributes == null) {
					throw new UcmServiceException(
						String.format("Revision ID [%s] was found via DOC_INFO(dID=%s), but returned empty results",
							wantedId, wantedId));
				}
				return cacheObject(this.revisionUriByRevisionID, rsp).getUniqueURI();
			});
		} catch (Exception e) {
			this.log.trace("getFileRevision({})", id, e);
			UcmModel.throwIfMatches(UcmServiceException.class, e);
			UcmModel.throwIfMatches(UcmFileRevisionNotFoundException.class, e);
			throw new UcmServiceException(String.format("Exception caught locating revision ID [%s]", id), e);
		}

		final UcmFSObject ret = this.objectByUniqueURI.get(guid);
		this.log.trace("getFileRevision({}) -> objectByUniqueURI.get({}) = {}", id, guid, ret);
		if (!UcmFile.class.isInstance(ret)) {
			throw new UcmFileRevisionNotFoundException(String.format("No revision found with ID [%s]", id));
		}
		return UcmFile.class.cast(ret);
	}

	protected Map<String, URI> getChildren(UcmSession s, String path)
		throws UcmServiceException, UcmFolderNotFoundException {
		try {
			return getFolderContents(s, resolvePath(s, path));
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
	}

	protected Map<String, URI> getFolderContents(UcmSession s, UcmUniqueURI guid)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(guid, "Must provide a BY_GUID to search for");
		// If it's a folder URI we already know the SSP is the BY_GUID, so... go!
		return getFolderContents(s, guid.getURI());
	}

	public Collection<URI> getURISearchResults(final UcmSession s, final String query) throws UcmServiceException {
		return getURISearchResults(s, query, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public Collection<URI> getURISearchResults(final UcmSession session, final String query, final int pageSize)
		throws UcmServiceException {
		final List<URI> results = new ArrayList<>();
		iterateURISearchResults(session, query, pageSize, (s, p, u) -> results.add(u));
		return results;
	}

	public long iterateURISearchResults(final UcmSession s, final String query,
		final CheckedTriConsumer<UcmSession, Long, URI, UcmServiceException> handler) throws UcmServiceException {
		return iterateURISearchResults(s, query, UcmConstants.DEFAULT_PAGE_SIZE, handler);
	}

	public long iterateURISearchResults(final UcmSession s, final String query, int pageSize,
		final CheckedTriConsumer<UcmSession, Long, URI, UcmServiceException> handler) throws UcmServiceException {
		Objects.requireNonNull(s, "Must provide a session to search with");
		Objects.requireNonNull(query, "Must provide a query to execute");
		Objects.requireNonNull(handler, "Must provide handler to use while iterating");

		// Syntax: query{sortAtts}[(startRow,)?rowCount(/pageSize)?] / sortAtts == +att1,-att2,...
		Matcher m = UcmModel.QUERY_PARSER.matcher(StringUtils.strip(query));
		if (!m.matches()) { throw new UcmServiceException(String.format("Invalid query syntax: [%s]", query)); }

		final String actualQuery = m.group(1);
		if (StringUtils.isEmpty(actualQuery)) {
			throw new UcmServiceException("The actual query string is empty - this is not supported");
		}

		final String sortField;
		final boolean desc;
		String sortSpecStr = m.group(2);
		if (sortSpecStr != null) {
			sortSpecStr = StringUtils.strip(sortSpecStr);
			Matcher sm = UcmModel.SORT_PARSER.matcher(sortSpecStr);
			if (!sm.matches()) {

			}

			sortField = sortSpecStr.replaceAll("^[-+]", "");
			desc = sortSpecStr.startsWith("-");
		} else {
			sortField = null;
			desc = false;
		}

		// TODO: This section supports multiple sort specifications - need to figure out why this
		// doesn't work...
		/*
		final StringBuilder sortSpec = new StringBuilder();
		if (sortSpecStr != null) {
			List<String> l = Tools.splitCSVEscaped(sortSpecStr);
			if (l.isEmpty()) { throw new UcmServiceException(String.format(
				"Illegal empty sort specification - the syntax is spec1[,spec2,spec3,...,specN] where specX is [-+]attributeName (+ = ASC, - = DESC, default is ASC)")); }
			for (String ss : l) {
				ss = StringUtils.strip(ss);
				Matcher sm = UcmModel.SORT_PARSER.matcher(ss);
				if (!sm.matches()) { throw new UcmServiceException(String.format(
					"Illegal attribute sort specification '%s' - the syntax is [-+]attributeName (+ = ASC, - = DESC, default is ASC)",
					ss)); }
				boolean desc = ss.startsWith("-");
				if (sortSpec.length() > 0) {
					sortSpec.append(", ");
				}
				if (ss.startsWith("-") || ss.startsWith("+")) {
					ss = ss.substring(1);
				}
				sortSpec.append(ss).append(' ').append(desc ? "De" : "A").append("sc");
			}
		}
		*/

		// TODO: Need to find the correct way to identify "automagically" if we should use database
		// or databasetext engines when running the search...if only there were documentation...
		final boolean dbMode = true;
		final AtomicLong startRow = new AtomicLong(1);
		final long maxRows;
		String rowSpec = m.group(3);
		if (rowSpec != null) {
			Matcher rm = UcmModel.ROW_PARSER.matcher(rowSpec);
			if (!rm.matches()) {
				throw new UcmServiceException(String.format(
					"Illegal row specification '%s' - the syntax is [startRow,]rowCount[/pageSize] where elements in [] are optional (numbers may not begin with a 0, and must be positive integers - the first row is 1)"));
			}
			String startRowStr = rm.group(1);
			if (startRowStr != null) {
				startRow.set(Long.parseLong(startRowStr));
			}
			maxRows = Integer.parseInt(rm.group(2));
			String pageSizeStr = rm.group(3);
			if (pageSizeStr != null) {
				pageSize = Integer.valueOf(pageSizeStr);
			}
		} else {
			maxRows = -1;
		}

		final int actualPageSize = Tools.ensureBetween(UcmConstants.MINIMUM_PAGE_SIZE, pageSize,
			UcmConstants.MAXIMUM_PAGE_SIZE);

		long rowNumber = 0;
		outer: while (true) {
			try {
				ServiceResponse response = s.callService("GET_SEARCH_RESULTS", (binder) -> {
					UcmModel.this.log.debug(
						"Calling GET_SEARCH_RESULTS (dbMode = {}, start row = {}, page size = {}, sortField = {} {}, query = [{}])",
						dbMode, startRow.get(), actualPageSize, sortField, desc ? "Desc" : "Asc", actualQuery);

					binder.putLocal("QueryText", actualQuery);
					if (dbMode) {
						binder.putLocal("SearchEngineName", "database");
					}
					binder.putLocal("StartRow", String.valueOf(startRow.get()));
					/*
					if (sortSpec.length() > 0) {
						binder.putLocal("SortSpec", sortSpec.toString());
					}
					*/
					if (sortField != null) {
						binder.putLocal("SortField", sortField);
						binder.putLocal("SortOrder", desc ? "Desc" : "Asc");
					}
					binder.putLocal("ResultCount", String.valueOf(actualPageSize));
					binder.putLocal("isAddFolderMetadata", "1");
				});
				DataBinder binder = response.getResponseAsBinder();
				DataResultSet results = binder.getResultSet("SearchResults");
				if (results == null) {
					break;
				}
				List<DataObject> rows = results.getRows();
				if ((rows == null) || rows.isEmpty()) {
					break;
				}
				final boolean lastPage = (rows.size() < actualPageSize);
				for (DataObject o : results.getRows()) {
					try {
						URI uri = UcmModel.getURI(new UcmAttributes(o, results.getFields()));
						handler.accept(s, ++rowNumber, uri);
					} finally {
						startRow.incrementAndGet();
						if ((maxRows > 0) && (rowNumber >= maxRows)) {
							break outer;
						}
					}
				}
				if (lastPage) {
					break outer;
				}
			} catch (final IdcClientException e) {
				throw new UcmServiceException(String.format("Exception raised while performing the %s query [%s]",
					dbMode ? "database" : "fulltext", actualQuery), e);
			}
		}
		return rowNumber;
	}

	@FunctionalInterface
	public static interface ObjectHandler {
		public void handleObject(UcmSession session, long pos, URI objectUri, UcmFSObject object);
	}

	public Collection<UcmFile> getDocumentSearchResults(final UcmSession s, final String query)
		throws UcmServiceException {
		return getDocumentSearchResults(s, query, UcmConstants.DEFAULT_PAGE_SIZE);
	}

	public Collection<UcmFile> getDocumentSearchResults(final UcmSession session, final String query,
		final int pageSize) throws UcmServiceException {
		final List<UcmFile> results = new ArrayList<>();
		iterateDocumentSearchResults(session, query, pageSize, (s, p, u, o) -> {
			if (UcmFile.class.isInstance(o)) {
				results.add(UcmFile.class.cast(o));
			}
		});
		return results;
	}

	public long iterateDocumentSearchResults(final UcmSession s, final String query, final ObjectHandler handler)
		throws UcmServiceException {
		return iterateDocumentSearchResults(s, query, UcmConstants.DEFAULT_PAGE_SIZE, handler);
	}

	public long iterateDocumentSearchResults(final UcmSession session, final String query, int pageSize,
		final ObjectHandler handler) throws UcmServiceException {
		Objects.requireNonNull(session, "Must provide a session to search with");
		Objects.requireNonNull(query, "Must provide a query to execute");
		Objects.requireNonNull(handler, "Must provide handler to use while iterating");

		return iterateURISearchResults(session, query, (s, p, u) -> {
			final UcmFile file;
			try {
				file = getFile(s, u);
			} catch (UcmFileNotFoundException e) {
				// The result was returned, but not accessible? KABOOM!
				throw new UcmServiceException(String.format(
					"The file with URI [%s] was returned in the result set, but was not retrieved when searched for explicitly",
					u), e);
			}
			handler.handleObject(s, p, file.getURI(), file);
		});
	}

	public int iterateFolderContents(final UcmSession s, final UcmFolder folder, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(folder, "Must provide a folder object to iterate over");
		return iterateFolderContents(s, folder.getURI(), handler);
	}

	int iterateFolderContents(final UcmSession s, final URI targetUri, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(s, "Must provide a session to search with");
		Objects.requireNonNull(targetUri, "Must provide a URI to search for");
		Objects.requireNonNull(handler, "Must provide handler to use while iterating");
		// If this isn't a folder, we don't even try it...
		if (!UcmModel.isFolderURI(targetUri)) { return -1; }

		Map<String, URI> children = this.childrenByURI.get(targetUri);
		if (children != null) {
			// We'll gather the objects first, and then iterate over them, because
			// if there's an inconsistency (i.e. a missing stale object), then we
			// want to do the full service invocation to the server
			Map<URI, UcmFSObject> objects = new LinkedHashMap<>(children.size());
			boolean reconstruct = false;
			for (URI childUri : children.values()) {
				try {
					objects.put(childUri, getFSObject(s, childUri));
				} catch (UcmObjectNotFoundException e) {
					reconstruct = true;
					break;
				}
			}

			// If the cache remains current, and we're not missing any child objects, then we quite
			// simply iterate over the objects we got and call it a day
			if (!reconstruct) {
				int ret = 0;
				for (URI childUri : objects.keySet()) {
					handler.handleObject(s, ret++, childUri, objects.get(childUri));
				}
				return ret;
			} else {
				// This ensures that the next thread (hopefully the current one) will trigger
				// the function in createIfAbsent(), just below here...
				this.childrenByURI.remove(targetUri);
			}
		}

		try {
			children = this.childrenByURI.computeIfAbsent(targetUri, (uri) -> {
				try {
					Map<String, URI> newChildren = new TreeMap<>();
					Map<String, UcmFSObject> dataObjects = new TreeMap<>();
					FolderContentsIterator it = new FolderContentsIterator(s, uri);
					while (it.hasNext()) {
						UcmAttributes o = it.next();
						URI childUri = UcmModel.getURI(o);
						String name = o.getString(UcmAtt.fFileName);
						if (name == null) {
							name = o.getString(UcmAtt.fFolderName);
						}
						newChildren.put(name, childUri);
						UcmFSObject childObject = newFSObject(childUri, o);
						dataObjects.put(name, childObject);
						long pos = it.getCurrentPos();
						handler.handleObject(s, pos, childUri, childObject);
					}
					UcmAttributes folderAtts = it.getFolder();
					cacheObject(this.childrenByURI, newFSObject(folderAtts));
					dataObjects.values().forEach(this::cacheObject);
					return newChildren;
				} catch (final UcmServiceException e) {
					Throwable cause = e.getCause();
					if (isNotFoundException(cause, "Exception caught retrieving the URI [%s]", uri)) {
						throw new UcmFolderNotFoundException(String.format("No folder found with URI [%s]", uri));
					}
					// This is a "regular" exception that we simply re-raise
					throw e;
				}
			});
		} catch (Exception e) {
			UcmModel.throwIfMatches(UcmServiceException.class, e);
			UcmModel.throwIfMatches(UcmFolderNotFoundException.class, e);
			throw new UcmServiceException(
				String.format("Exception caught finding the folder contents for URI [%s]", targetUri), e);
		}

		return (children != null ? children.size() : 0);
	}

	protected Map<String, URI> getFolderContents(UcmSession session, final URI uri)
		throws UcmServiceException, UcmFolderNotFoundException {
		final Map<String, URI> children = new LinkedHashMap<>();
		iterateFolderContents(session, uri, (s, p, u, o) -> children.put(o.getName(), u));
		return children;
	}

	public Map<String, UcmFSObject> getFolderContents(UcmSession session, final UcmFolder folder)
		throws UcmServiceException, UcmFolderNotFoundException {
		final Map<String, UcmFSObject> children = new LinkedHashMap<>();
		iterateFolderContents(session, folder.getURI(), (s, p, u, o) -> children.put(o.getName(), o));
		return children;
	}

	private int iterateFolderContentsRecursive(final Set<URI> recursions, final AtomicInteger outerPos,
		final UcmSession session, final URI uri, final boolean followShortcuts, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to search for");
		Objects.requireNonNull(handler, "Must provide handler to use while iterating");
		// If this isn't a folder, we don't even try it...
		if (!UcmModel.isFolderURI(uri)) { return -1; }

		if (recursions.isEmpty()) {
			// If this is the root of the invocation, we handle it!
			handler.handleObject(session, outerPos.getAndIncrement(), uri, getFolder(session, uri));
		}

		if (!recursions.add(uri)) {
			throw new IllegalStateException(
				String.format("Folder recursion detected when descending into [%s] : %s", uri, recursions));
		}

		try {
			Set<URI> recursables = new LinkedHashSet<>();
			iterateFolderContents(session, uri, (s, p, u, o) -> {
				handler.handleObject(s, outerPos.getAndIncrement(), u, o);
				if (UcmModel.isFolderURI(u) && (followShortcuts || !o.isShortcut())) {
					recursables.add(u);
				}
			});
			// now, do the recursions
			recursables.forEach((u) -> {
				try {
					iterateFolderContentsRecursive(recursions, outerPos, session, u, followShortcuts, handler);
				} catch (UcmFolderNotFoundException e) {
					throw new UcmRuntimeException(String
						.format("Unexpected condition: can't find a folder that has just been found?? URI=[%s]", u), e);
				} catch (UcmServiceException e) {
					throw new UcmRuntimeServiceException(String.format(
						"Service exception caught while attempting to recurse through [%s] : %s", u, recursions), e);
				}
			});
			return outerPos.get();
		} catch (UcmRuntimeServiceException e) {
			throw new UcmServiceException(String.format("Service exception caught while recursing through [%s]", uri),
				e);
		} finally {
			recursions.remove(uri);
		}
	}

	public int iterateFolderContentsRecursive(final UcmSession s, final UcmFolder folder, boolean followShortCuts,
		final ObjectHandler handler) throws UcmServiceException, UcmFolderNotFoundException {
		return iterateFolderContentsRecursive(s, folder.getURI(), followShortCuts, handler);
	}

	int iterateFolderContentsRecursive(final UcmSession s, final URI uri, boolean followShortCuts,
		final ObjectHandler handler) throws UcmServiceException, UcmFolderNotFoundException {
		return iterateFolderContentsRecursive(new LinkedHashSet<URI>(), new AtomicInteger(0), s, uri, followShortCuts,
			handler);
	}

	Collection<URI> getFolderContentsRecursive(UcmSession session, boolean followShortCuts, final URI uri)
		throws UcmServiceException, UcmFolderNotFoundException {
		final Collection<URI> children = new ArrayList<>();
		iterateFolderContentsRecursive(session, uri, followShortCuts, (s, p, u, o) -> children.add(u));
		return children;
	}

	public Collection<UcmFSObject> getFolderContentsRecursive(UcmSession session, final UcmFolder folder,
		boolean followShortCuts) throws UcmServiceException, UcmFolderNotFoundException {
		final Collection<UcmFSObject> children = new ArrayList<>();
		iterateFolderContentsRecursive(session, folder.getURI(), followShortCuts, (s, p, u, o) -> children.add(o));
		return children;
	}

	public UcmFolder getRootFolder(UcmSession s) throws UcmServiceException {
		try {
			return getFolder(s, UcmModel.ROOT_URI);
		} catch (UcmFolderNotFoundException e) {
			throw new UcmServiceException("Could not find the root folder - this appears to be a server problem", e);
		}
	}

	public UcmFolder getFolder(UcmSession s, UcmUniqueURI uri) throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a unique URI to locate");
		return getFolder(s, uri.getURI());
	}

	public UcmFolder getFolder(UcmSession s, String path) throws UcmServiceException, UcmFolderNotFoundException {
		URI uri = null;
		try {
			uri = resolvePath(s, path);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
		return getFolder(s, uri);
	}

	public UcmFolder getFolder(UcmSession s, URI uri) throws UcmServiceException, UcmFolderNotFoundException {
		Objects.requireNonNull(uri, "Must provide a URI to locate");
		if (!UcmModel.isFolderURI(uri)) {
			throw new UcmFolderNotFoundException(String.format("The object URI [%s] is not a folder URI", uri));
		}
		if (UcmModel.NULL_FOLDER_URI.equals(uri)) { return null; }
		UcmFSObject data = null;
		try {
			data = getFSObject(s, uri);
			if (UcmFolder.class.isInstance(data)) { return UcmFolder.class.cast(data); }
			data = getFSObject(s, uri);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFolderNotFoundException(e.getMessage(), e);
		}
		throw new UcmFolderNotFoundException(String.format("The object with URI [%s] is not a folder: %s", uri, data));
	}

	public UcmFileHistory getFileHistory(UcmSession s, String path)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileHistory(s, getFile(s, path));
	}

	public UcmFileHistory getFileHistory(UcmSession s, UcmFile file)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		return getFileHistory(s, file.getURI(), file.getRevisionId());
	}

	UcmFileHistory getFileHistory(UcmSession s, URI uri)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		final UcmFSObject file;
		try {
			file = getFSObject(s, uri);
		} catch (UcmObjectNotFoundException e) {
			throw new UcmFileNotFoundException(e.getMessage(), e);
		}

		return getFileHistory(s, uri, file.getString(UcmAtt.dID));
	}

	private UcmFileHistory getFileHistory(final UcmSession s, final URI uri, final String revisionId)
		throws UcmServiceException, UcmFileNotFoundException, UcmFileRevisionNotFoundException {
		final List<UcmRevision> history;
		try {
			history = this.historyByURI.computeIfAbsent(uri, (targetUri) -> {
				final ServiceResponse response;
				final DataBinder responseData;
				try {
					response = s.callService("REV_HISTORY", (binder) -> binder.putLocal("dID", revisionId));
					responseData = response.getResponseAsBinder();
				} catch (final IdcClientException e) {
					if (isNotFoundException(e, "Exception caught retrieving the URI [%s]", targetUri)) {
						throw new UcmFileNotFoundException(String.format("No file found with URI [%s]", targetUri), e);
					}
					// This is a "regular" exception that we simply re-raise
					throw e;
				}

				final DataResultSet revisions = responseData.getResultSet("REVISIONS");
				LinkedList<UcmRevision> newHistory = new LinkedList<>();
				for (DataObject o : revisions.getRows()) {
					newHistory.addFirst(new UcmRevision(targetUri, o, revisions.getFields()));
				}
				return newHistory;
			});
		} catch (Exception e) {
			UcmModel.throwIfMatches(UcmServiceException.class, e);
			UcmModel.throwIfMatches(UcmFileNotFoundException.class, e);
			throw new UcmServiceException(String.format("Exception caught finding the file history for URI [%s]", uri),
				e);
		}

		return new UcmFileHistory(this, uri, history);
	}

	public InputStream getInputStream(UcmSession s, final UcmFile file, final String rendition)
		throws UcmServiceException, UcmFileRevisionNotFoundException, UcmRenditionNotFoundException {
		ServiceResponse response = s.callService("GET_FILE", (binder) -> {
			binder.putLocal("dID", file.getRevisionId());
			if (!StringUtils.isEmpty(rendition)) {
				binder.putLocal("Rendition", rendition.toUpperCase());
			}
		});
		if (response.getResponseType() == ResponseType.STREAM) { return response.getResponseStream(); }

		// Ok...there was a problem... what was the problem?
		final DataBinder binder;
		try {
			binder = response.getResponseAsBinder();
		} catch (IdcClientException e) {
			throw new UcmServiceException(String.format(
				"Failed to decode the service response when invoking GET_FILE for rendition [%s] from file [%s]",
				Tools.coalesce(rendition, UcmModel.RENDITION_DEFAULT_TYPE), file.getUniqueURI()), e);
		}

		DataObject local = binder.getLocalData();
		int status = local.getInteger("StatusCode");
		if (status == -16) {
			// Resource not found...so the revision wasn't found
			throw new UcmFileRevisionNotFoundException(
				String.format("File revision [%s] was not found", file.getUniqueURI()));
		}

		if (status == -32) {
			// Procedural error - so the revision was found, but not the rendition...maybe?
			if (local.get("StatusMessageKey").indexOf("!csGetFileRenditionNotFound,") >= 0) {
				// Rendition not found!
				throw new UcmRenditionNotFoundException(String.format("Rendition [%s] not found for file [%s]",
					Tools.coalesce(rendition, UcmModel.RENDITION_DEFAULT_TYPE), file.getUniqueURI()));
			}
		}

		// Some other error!
		throw new UcmServiceException(String.format("Failed to load rendition [%s] from file [%s]",
			Tools.coalesce(rendition, UcmModel.RENDITION_DEFAULT_TYPE), file.getUniqueURI()));
	}

	static String sanitizePath(String path) {
		Objects.requireNonNull(path, "Must provide a non-null path");
		if (!UcmModel.PATH_CHECKER.matcher(path).matches()) {
			// Single separators...
			path = path.replaceAll("/+", "/");
			// Make sure there's a leading separator
			if (!path.startsWith("/")) {
				path = String.format("/%s", path);
			}
		}
		String newPath = FilenameUtils.normalizeNoEndSeparator(path, true);
		if (newPath == null) {
			throw new IllegalArgumentException(
				String.format("The given path [%s] is invalid - too may '..' elements", path));
		}
		return newPath;
	}

	static <T extends Throwable> void throwIfMatches(Class<T> k, Throwable t) throws T {
		Objects.requireNonNull(k, "Must provide an exception class to evaluate");
		if (k.isInstance(t)) { throw k.cast(t); }
	}

	UcmFile refresh(UcmSession s, UcmFile f)
		throws UcmFileNotFoundException, UcmServiceException, UcmFileRevisionNotFoundException {
		return getFileRevision(s, f);
	}

	UcmFolder refresh(UcmSession s, UcmFolder f) throws UcmFolderNotFoundException, UcmServiceException {
		return getFolder(s, f.getUniqueURI());
	}

	UcmFileHistory refresh(UcmSession s, UcmFileHistory h)
		throws UcmFileNotFoundException, UcmServiceException, UcmFileRevisionNotFoundException {
		return getFileHistory(s, h.getURI());
	}

	private UcmRenditionInfo generateDefaultRendition(UcmUniqueURI guid, UcmAttributes attributes) {
		String format = attributes.getString(UcmAtt.dFormat);
		if (format == null) {
			format = UcmModel.REMDITION_DEFAULT_FORMAT;
		}
		return new UcmRenditionInfo(guid, UcmModel.RENDITION_DEFAULT_TYPE, format, "Native File",
			"The original format (added automatically)");
	}

	public Map<String, UcmRenditionInfo> getRenditions(final UcmSession s, final UcmFile file)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		Objects.requireNonNull(file, "Must provide a file whose renditions to return");

		final UcmUniqueURI guid = file.getUniqueURI();
		final String id = file.getRevisionId();
		final Map<String, UcmRenditionInfo> renditions;
		try {
			renditions = this.renditionsByUniqueURI.computeIfAbsent(guid, (fileGuid) -> {
				ServiceResponse response = null;
				DataBinder responseData = null;
				try {
					response = s.callService("DOC_INFO", (binder) -> {
						binder.putLocal("dID", id);
						binder.putLocal("includeFileRenditionsInfo", "1");
						binder.putLocal("isAddFolderMetadata", "1");
					});
					responseData = response.getResponseAsBinder();
				} catch (final IdcClientException e) {
					if (isNotFoundException(e, "Exception caught retrieving the revision with ID [%s]", id)) {
						throw new UcmFileRevisionNotFoundException();
					}
					// This is a "regular" exception that we simply re-raise
					throw e;
				}

				final UcmServiceResponse rsp = buildAttributesFromDocInfo(responseData);
				Map<String, UcmRenditionInfo> newRenditions = new TreeMap<>();
				DataResultSet rs = rsp.getRenditions();
				if (rs != null) {
					for (DataObject o : rs.getRows()) {
						UcmRenditionInfo r = new UcmRenditionInfo(fileGuid, o, rs.getFields());
						newRenditions.put(r.getType().toUpperCase(), r);
					}
				} else {
					UcmModel.this.log.warn(
						"Revision ID [{}] was found via DOC_INFO(dID={}), but no rendition information was returned??! Generated a default primary rendition",
						id, id);
					UcmAttributes attributes = rsp.getAttributes();
					UcmRenditionInfo info = generateDefaultRendition(fileGuid, attributes);
					newRenditions.put(info.getType(), info);
				}
				cacheObject(this.renditionsByUniqueURI, rsp);
				return newRenditions;
			});
		} catch (Exception e) {
			UcmModel.throwIfMatches(UcmServiceException.class, e);
			UcmModel.throwIfMatches(UcmFileRevisionNotFoundException.class, e);
			throw new UcmServiceException(
				String.format("Exception caught retrieving the renditions list for file [%s] (revision ID [%s]",
					file.getURI(), file.getRevisionId()),
				e);
		}
		return new TreeMap<>(renditions);
	}

}