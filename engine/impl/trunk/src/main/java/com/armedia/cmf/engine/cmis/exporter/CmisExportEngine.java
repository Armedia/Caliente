package com.armedia.cmf.engine.cmis.exporter;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Rendition;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;

import com.armedia.cmf.engine.ContentInfo;
import com.armedia.cmf.engine.cmis.CmisCommon;
import com.armedia.cmf.engine.cmis.CmisPagingTransformerIterator;
import com.armedia.cmf.engine.cmis.CmisRecursiveIterator;
import com.armedia.cmf.engine.cmis.CmisResultTransformer;
import com.armedia.cmf.engine.cmis.CmisSessionFactory;
import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.cmis.CmisSetting;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ContentStore.Handle;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportEngine extends
ExportEngine<Session, CmisSessionWrapper, CmisObject, Property<?>, CmisExportContext> {

	private final CmisResultTransformer<QueryResult, ExportTarget> transformer = new CmisResultTransformer<QueryResult, ExportTarget>() {
		@Override
		public ExportTarget transform(QueryResult result) throws Exception {
			return newExportTarget(result);
		}
	};

	@Override
	protected String getObjectId(CmisObject sourceObject) {
		return sourceObject.getId();
	}

	@Override
	protected String calculateLabel(CmisObject obj) throws Exception {
		if (obj instanceof FileableCmisObject) {
			FileableCmisObject f = FileableCmisObject.class.cast(obj);
			List<String> paths = f.getPaths();
			if (!paths.isEmpty()) { return paths.get(0); }
			return String.format("${unfiled}:%s", obj.getName());
		}
		return String.format("[%s|%s]", obj.getType().getId(), obj.getName());
	}

	protected ExportTarget newExportTarget(QueryResult r) throws ExportException {
		PropertyData<?> objectId = r.getPropertyById(PropertyIds.OBJECT_ID);
		if (objectId == null) { throw new ExportException(
			"Failed to find the cmis:objectId property as part of the query result"); }

		StoredObjectType type = null;
		PropertyData<?>[] objectTypes = {
			r.getPropertyById(PropertyIds.OBJECT_TYPE_ID), r.getPropertyById(PropertyIds.BASE_TYPE_ID)
		};

		for (PropertyData<?> t : objectTypes) {
			if (t == null) {
				continue;
			}
			if (this.log.isTraceEnabled()) {
				this.log.trace(String.format("Found property [%s] with value [%s]", t.getId(), t.getFirstValue()));
			}
			type = decodeType(Tools.toString(t.getFirstValue()));
			if (type != null) {
				if (this.log.isTraceEnabled()) {
					this.log.trace(String.format("Object type [%s] decoded as [%s]", t.getFirstValue(), type));
				}
				break;
			}
		}
		if (type == null) { throw new ExportException(
			"Failed to find the cmis:objectTypeId or the cmis:baseTypeId properties as part of the query result, or the returned value couldn't be decoded"); }
		String id = Tools.toString(objectId.getFirstValue());
		return new ExportTarget(type, id, id);
	}

	@Override
	protected Iterator<ExportTarget> findExportResults(final Session session, Map<String, ?> settings) throws Exception {
		CfgTools cfg = new CfgTools(settings);
		String path = cfg.getString(CmisSetting.EXPORT_PATH);
		if (path != null) {
			final CmisObject obj;
			try {
				obj = session.getObjectByPath(path);
			} catch (CmisObjectNotFoundException e) {
				return null;
			}
			if (obj instanceof Folder) {
				return new Iterator<ExportTarget>() {
					private final Iterator<CmisObject> it = new CmisRecursiveIterator(session, Folder.class.cast(obj),
						true);

					@Override
					public boolean hasNext() {
						return this.it.hasNext();
					}

					@Override
					public ExportTarget next() {
						final CmisObject next = this.it.next();
						try {
							return new ExportTarget(decodeType(next.getType()), next.getId(), next.getId());
						} catch (ExportException e) {
							throw new RuntimeException(String.format(
								"Failed to decode the object type [%s] for object [%s]", next.getType().getId(),
								next.getId()), e);
						}
					}

					@Override
					public void remove() {
						this.it.remove();
					}
				};
			} else {
				try {
					return Collections.singleton(
						new ExportTarget(decodeType(obj.getBaseType()), obj.getId(), obj.getId())).iterator();
				} catch (CmisObjectNotFoundException e) {
					return null;
				}
			}
		}

		final String query = cfg.getString(CmisSetting.EXPORT_QUERY);
		if (query != null) {
			final int itemsPerPage = Math.max(10, cfg.getInteger(CmisSetting.EXPORT_QUERY_PAGE_SIZE));
			final OperationContext ctx = session.createOperationContext();
			ctx.setMaxItemsPerPage(itemsPerPage);
			final boolean searchAllVersions = session.getRepositoryInfo().getCapabilities()
				.isAllVersionsSearchableSupported();
			return new CmisPagingTransformerIterator<QueryResult, ExportTarget>(session.query(query, searchAllVersions,
				ctx), this.transformer);
		}
		return null;
	}

	@Override
	protected CmisObject getObject(Session session, StoredObjectType type, String searchKey) throws Exception {
		return session.getObject(searchKey);
	}

	@Override
	protected Collection<CmisObject> identifyRequirements(Session session, StoredObject<Property<?>> marshalled,
		CmisObject object, CmisExportContext ctx) throws Exception {
		return Collections.emptyList();
	}

	@Override
	protected Collection<CmisObject> identifyDependents(Session session, StoredObject<Property<?>> marshalled,
		CmisObject object, CmisExportContext ctx) throws Exception {
		return Collections.emptyList();
	}

	@Override
	protected ExportTarget getExportTarget(CmisObject object) throws ExportException {
		return new ExportTarget(decodeType(object.getType()), object.getId(), object.getId());
	}

	protected StoredObjectType decodeType(ObjectType type) throws ExportException {
		if (!type.isBaseType()) { return decodeType(type.getParentType()); }
		return decodeType(type.getId());
	}

	protected StoredObjectType decodeType(String type) throws ExportException {
		if (Tools.equals("cmis:folder", type)) { return StoredObjectType.FOLDER; }
		if (Tools.equals("cmis:document", type)) { return StoredObjectType.DOCUMENT; }
		return null;
	}

	@Override
	protected StoredObject<Property<?>> marshal(CmisExportContext ctx, Session session, CmisObject object)
		throws ExportException {
		return null;
	}

	@Override
	protected List<ContentInfo> storeContent(Session session, StoredObject<Property<?>> marshaled,
		ExportTarget referrent, CmisObject object, ContentStore streamStore) throws Exception {
		if (session == null) { throw new IllegalArgumentException(
			"Must provide a session through which to store the contents"); }
		if (marshaled == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		if (object == null) { throw new IllegalArgumentException("Must provide an object whose contents to store"); }
		if (streamStore == null) { throw new IllegalArgumentException(
			"Must provide a stream store in which to store the content"); }
		List<ContentInfo> info = null;
		if (object instanceof Document) {
			info = new ArrayList<ContentInfo>();
			Document d = Document.class.cast(object);
			for (Rendition r : d.getRenditions()) {
				Handle h = streamStore.getHandle(marshaled, URLEncoder.encode(r.getKind(), "UTF-8"));
				ContentStream c = r.getContentStream();
				OutputStream out = null;
				InputStream in = null;
				try {
					out = h.openOutput();
					in = c.getStream();
					IOUtils.copy(in, out);
				} finally {
					IOUtils.closeQuietly(out);
					IOUtils.closeQuietly(in);
				}
			}
		}
		return info;
	}

	@Override
	protected Property<?> getValue(StoredDataType type, Object value) {
		return null;
	}

	@Override
	protected ObjectStorageTranslator<CmisObject, Property<?>> getTranslator() {
		return null;
	}

	@Override
	protected CmisSessionFactory newSessionFactory(CfgTools cfg) throws Exception {
		return new CmisSessionFactory(cfg);
	}

	@Override
	protected CmisExportContextFactory newContextFactory(CfgTools cfg) throws Exception {
		return new CmisExportContextFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return CmisCommon.TARGETS;
	}

	public static ExportEngine<?, ?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(CmisCommon.TARGET_NAME);
	}
}