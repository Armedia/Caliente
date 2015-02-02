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

import org.apache.commons.lang3.StringUtils;

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
import com.independentsoft.share.SearchResult;
import com.independentsoft.share.SearchResultPropertyName;
import com.independentsoft.share.Service;
import com.independentsoft.share.SimpleDataRow;
import com.independentsoft.share.fql.IRestriction;
import com.independentsoft.share.fql.IsEqualTo;
import com.independentsoft.share.fql.Or;

/**
 * @author diego
 *
 */
public class ShptExportEngine extends
	ExportEngine<Service, ShptSessionWrapper, ShptObject<?>, StoredValue, ShptExportContext> {

	private static final List<ExportTarget> NO_TARGETS = Collections.emptyList();
	private static final Set<String> TARGETS = Collections.singleton(ShptObject.TARGET_NAME);

	private class ExportTargetIterator implements Iterator<ExportTarget> {
		private final Iterator<SimpleDataRow> it;

		private ExportTargetIterator(ResultTable rt) {
			this.it = rt.getTable().getRows().iterator();
		}

		@Override
		public boolean hasNext() {
			return this.it.hasNext();
		}

		@Override
		public ExportTarget next() {
			SimpleDataRow r = this.it.next();
			for (KeyValue kv : r.getCells()) {
				if (Tools.equals("UniqueId", kv.getKey())) { return new ExportTarget(null, kv.getValue()); }
			}
			throw new RuntimeException(String.format(
				"Failed to identify the UniqueId property for the current object: %s", r));
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

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
		// support query by path (i.e. all files in these paths)
		// support query by Sharepoint query language
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to retrieve the results"); }
		if (settings == null) {
			settings = Collections.emptyMap();
		}
		Object query = settings.get("query");
		// List<> of Strings representing paths
		Object pathList = settings.get("paths");
		// List<> of Strings representing guids
		Object guid = settings.get("guids");

		final SearchResult result;
		if (query != null) {
			result = session.search(query.toString());
		} else if ((pathList != null) || (guid != null)) {
			Object list = (pathList != null ? pathList : guid);
			if (!List.class.isInstance(list)) { throw new Exception(String.format(
				"The %s list must be of type java.util.List", (pathList != null ? "path" : "guid"))); }
			final List<?> l = List.class.cast(list);
			if (l.isEmpty()) { throw new Exception(String.format("Must provide at least one %s to export",
				(pathList != null ? "path" : "guid"))); }
			final List<IRestriction> restrictions = new ArrayList<IRestriction>(l.size());
			final String property = (pathList != null ? SearchResultPropertyName.PATH : "UniqueId");
			for (Object o : l) {
				final String s = Tools.toString(o, true);
				if (StringUtils.isEmpty(s)) {
					continue;
				}
				restrictions.add(new IsEqualTo(property, s));
			}
			result = session.search(new Or(restrictions));
		} else {
			throw new Exception("Must provide the criteria to search with");
		}
		ResultTable rt = result.getPrimaryQueryResult().getRelevantResult();
		if (rt.getRowCount() == 0) { return ShptExportEngine.NO_TARGETS.iterator(); }
		return new ExportTargetIterator(rt);
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
				return new ShptFile(session, session.getFile(id));
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
	protected Handle storeContent(Service session, StoredObject<StoredValue> marshaled, ShptObject<?> object,
		ContentStore streamStore) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to store the contents"); }
		if (marshaled == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		ExportTarget referrent = getReferrent(marshaled);
		if (referrent != null) {
			referrent.hashCode();
		}
		// TODO: Actually store the content
		return null;
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