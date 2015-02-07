/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.Setting;
import com.armedia.cmf.engine.sharepoint.ShptException;
import com.armedia.cmf.engine.sharepoint.ShptSessionFactory;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.engine.sharepoint.ShptTranslator;
import com.armedia.cmf.engine.sharepoint.types.ShptFile;
import com.armedia.cmf.engine.sharepoint.types.ShptFolder;
import com.armedia.cmf.engine.sharepoint.types.ShptGroup;
import com.armedia.cmf.engine.sharepoint.types.ShptObject;
import com.armedia.cmf.engine.sharepoint.types.ShptUser;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.File;
import com.independentsoft.share.Folder;
import com.independentsoft.share.Service;
import com.independentsoft.share.ServiceException;

/**
 * @author diego
 *
 */
public class ShptExportEngine extends
ExportEngine<Service, ShptSessionWrapper, ShptObject<?>, StoredValue, ShptExportContext> {

	private static final Set<String> TARGETS = Collections.singleton(ShptObject.TARGET_NAME);

	@Override
	protected String getObjectId(ShptObject<?> sourceObject) {
		return sourceObject.getId();
	}

	@Override
	protected String calculateLabel(ShptObject<?> sourceObject) throws Exception {
		return sourceObject.getLabel();
	}

	@Override
	protected Iterator<ExportTarget> findExportResults(Service service, Map<String, ?> settings) throws Exception {
		// support query by path (i.e. all files in these paths)
		// support query by Sharepoint query language
		if (service == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		if (settings == null) {
			settings = Collections.emptyMap();
		}
		final String path = CfgTools.decodeString(Setting.PATH, settings);
		if (path == null) { throw new ShptException("Must provide the name of the site to export"); }
		final boolean excludeEmptyFolders = CfgTools.decodeBoolean(Setting.EXCLUDE_EMPTY_FOLDERS, settings);

		/*
		Contains contains = new Contains(SearchResultPropertyName.PATH, service.getSiteUrl());
		SearchQuery query = new SearchQuery(contains);
		query.getSelectProperties().add(SearchResultPropertyName.PATH);
		// query.getSelectProperties().add(SearchResultPropertyName.IS_CONTAINER);
		// query.getSelectProperties().add(SearchResultPropertyName.IS_DOCUMENT);
		SearchResult result = service.search(query);
		SimpleDataTable table = result.getPrimaryQueryResult().getRelevantResult().getTable();

		int i = 0;
		for (SimpleDataRow row : table.getRows()) {
			this.log.debug("\t\tRow #{}", i++);
			for (KeyValue cell : row.getCells()) {
				this.log.debug("\t\t\t{} = [{}]", cell.getKey(), cell.getValue());
			}
		}
		 */

		this.log.trace("Starting recursive search of [{}]...", path);
		ShptFolder folder = new ShptFolder(service, service.getFolder(path));
		Collection<ExportTarget> ret = new ArrayList<ExportTarget>();
		try {
			addItemsRecursively(ret, folder.getServerRelativeUrl(), folder, excludeEmptyFolders);
		} catch (ServiceException e) {
			throw new ShptException(String.format("Export target search failed with URL [%s]", e.getRequestUrl()), e);
		}
		return ret.iterator();
	}

	private void addItemsRecursively(Collection<ExportTarget> c, String root, ShptFolder folder,
		final boolean excludeEmptyFolders) throws ServiceException {
		String url = folder.getServerRelativeUrl();
		// We don't add a slash b/c all folders' relative URLs end with a slash
		if (url.startsWith(String.format("%s_cts", root))) {
			this.log.trace("Skipping [{}]", url);
			return;
		}
		this.log.trace("Exploring contents of: [{}]", url);
		final Service service = folder.getService();
		List<File> files = service.getFiles(url);
		for (File f : files) {
			ShptFile F = new ShptFile(service, f);
			this.log.trace("\tExporting file: [{}]", f.getServerRelativeUrl());
			c.add(new ExportTarget(StoredObjectType.DOCUMENT, F.getId(), F.getSearchKey()));
		}

		List<Folder> folders = service.getFolders(url);
		for (Folder f : folders) {
			addItemsRecursively(c, root, new ShptFolder(service, f), excludeEmptyFolders);
		}

		if (!excludeEmptyFolders && folders.isEmpty() && files.isEmpty()) {
			// We add the caller if and only if there are neither files nor folders
			this.log.trace("\tExporting folder [{}] (the folder is empty)", folder.getServerRelativeUrl());
			c.add(new ExportTarget(StoredObjectType.FOLDER, folder.getId(), folder.getServerRelativeUrl()));
		}
	}

	@Override
	protected ShptObject<?> getObject(Service session, StoredObjectType type, String id) throws Exception {
		switch (type) {
			case USER:
				return new ShptUser(session, session.getUser(Tools.decodeInteger(id)));
			case GROUP:
				return new ShptGroup(session, session.getGroup(Tools.decodeInteger(id)));
			case FOLDER:
				return new ShptFolder(session, session.getFolder(id));
			case DOCUMENT:
				return ShptFile.locateFile(session, id);
			default:
				throw new Exception(String.format("Unsupported object type [%s]", type));
		}
	}

	@Override
	protected Collection<ShptObject<?>> identifyRequirements(Service session, StoredObject<StoredValue> marshalled,
		ShptObject<?> object, ShptExportContext ctx) throws Exception {
		return object.identifyRequirements(session, marshalled, ctx);
	}

	@Override
	protected Collection<ShptObject<?>> identifyDependents(Service session, StoredObject<StoredValue> marshalled,
		ShptObject<?> object, ShptExportContext ctx) throws Exception {
		return object.identifyDependents(session, marshalled, ctx);
	}

	@Override
	protected ExportTarget getExportTarget(ShptObject<?> object) throws ExportException {
		return new ExportTarget(object.getStoredType(), object.getId(), object.getSearchKey());
	}

	@Override
	protected StoredObject<StoredValue> marshal(ShptExportContext ctx, Service session, ShptObject<?> object)
		throws ExportException {
		return object.marshal();
	}

	@Override
	protected Handle storeContent(Service session, StoredObject<StoredValue> marshaled, ExportTarget referrent,
		ShptObject<?> object, ContentStore streamStore) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to store the contents"); }
		if (marshaled == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		if (streamStore == null) { throw new IllegalArgumentException(
			"Must provide a stream store in which to store the content"); }
		return object.storeContent(session, marshaled, streamStore);
	}

	@Override
	protected StoredValue getValue(StoredDataType type, Object value) {
		try {
			return new StoredValue(type, value);
		} catch (ParseException e) {
			throw new RuntimeException("Exception raised while creating a new value", e);
		}
	}

	@Override
	protected ObjectStorageTranslator<ShptObject<?>, StoredValue> getTranslator() {
		return ShptTranslator.INSTANCE;
	}

	@Override
	protected ShptSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new ShptSessionFactory(cfg);
	}

	@Override
	protected ShptExportContextFactory newContextFactory(CfgTools cfg) throws Exception {
		return new ShptExportContextFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return ShptExportEngine.TARGETS;
	}
}