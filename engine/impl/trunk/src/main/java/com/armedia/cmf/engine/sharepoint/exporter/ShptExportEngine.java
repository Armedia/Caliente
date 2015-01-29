/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.sharepoint.ShptFile;
import com.armedia.cmf.engine.sharepoint.ShptFolder;
import com.armedia.cmf.engine.sharepoint.ShptGroup;
import com.armedia.cmf.engine.sharepoint.ShptObject;
import com.armedia.cmf.engine.sharepoint.ShptSessionFactory;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.engine.sharepoint.ShptTranslator;
import com.armedia.cmf.engine.sharepoint.ShptUser;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;
import com.independentsoft.share.KeyValue;
import com.independentsoft.share.ResultTable;
import com.independentsoft.share.SearchQuery;
import com.independentsoft.share.SearchResult;
import com.independentsoft.share.SearchResultPropertyName;
import com.independentsoft.share.Service;
import com.independentsoft.share.SimpleDataRow;
import com.independentsoft.share.fql.IsEqualTo;

/**
 * @author diego
 *
 */
public class ShptExportEngine extends
	ExportEngine<Service, ShptSessionWrapper, ShptObject<?>, StoredValue, ShptExportContext> {

	@Override
	protected String getObjectId(ShptObject<?> sourceObject) {
		return sourceObject.getId();
	}

	@Override
	protected String calculateLabel(ShptObject<?> sourceObject) throws Exception {
		return sourceObject.getLabel();
	}

	@Override
	protected Iterator<ExportTarget> findExportResults(Service session, Map<String, ?> settings) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ShptObject<?> getObject(Service session, StoredObjectType type, String id) throws Exception {
		switch (type) {
			case USER:
				return new ShptUser(session, session.getUser(Tools.decodeInteger(id)));
			case GROUP:
				return new ShptGroup(session, session.getGroup(Tools.decodeInteger(id)));
			case DOCUMENT:
			case FOLDER:
				SearchQuery query = new SearchQuery(new IsEqualTo("UniqueId", id));
				query.getSelectProperties().add(SearchResultPropertyName.PATH);
				query.getSelectProperties().add(SearchResultPropertyName.TITLE);
				query.getSelectProperties().add("UniqueId");
				SearchResult searchResult = session.search(query);
				ResultTable results = searchResult.getPrimaryQueryResult().getRelevantResult();
				final int rows = results.getRowCount();
				if (rows == 0) { return null; }
				if (rows > 1) { throw new Exception(String.format("%s id [%s] returned %d results", type, id, rows)); }
				for (SimpleDataRow row : results.getTable().getRows()) {
					for (KeyValue kv : row.getCells()) {
						if (Tools.equals(kv.getKey(), SearchResultPropertyName.PATH)) { return (type == StoredObjectType.FOLDER ? new ShptFolder(
							session, session.getFolder(kv.getValue())) : new ShptFile(session, session.getFile(kv
								.getValue()))); }
					}
					// If the search didn't return the object's path, then we can't return it...
					throw new Exception(String.format(
						"Failed to locate the PATH property for the query for %s with ID [%s]", type, id));
				}
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
		return new ExportTarget(object.getStoredType(), object.getId());
	}

	@Override
	protected StoredObject<StoredValue> marshal(ShptExportContext ctx, Service session, ShptObject<?> object)
		throws ExportException {
		return object.marshal();
	}

	@Override
	protected Handle storeContent(Service session, StoredObject<StoredValue> marshalled, ShptObject<?> object,
		ContentStore streamStore) throws Exception {
		return null;
	}

	@Override
	protected StoredValue getValue(StoredDataType type, Object value) {
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}
}