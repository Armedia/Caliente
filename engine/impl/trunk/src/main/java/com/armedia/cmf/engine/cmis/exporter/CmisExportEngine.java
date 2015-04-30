package com.armedia.cmf.engine.cmis.exporter;

import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;

import com.armedia.cmf.engine.cmis.CmisAcl;
import com.armedia.cmf.engine.cmis.CmisCommon;
import com.armedia.cmf.engine.cmis.CmisObjectStorageTranslator;
import com.armedia.cmf.engine.cmis.CmisPagingTransformerIterator;
import com.armedia.cmf.engine.cmis.CmisRecursiveIterator;
import com.armedia.cmf.engine.cmis.CmisResultTransformer;
import com.armedia.cmf.engine.cmis.CmisSessionFactory;
import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.cmis.CmisSetting;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportEngine extends ExportEngine<Session, CmisSessionWrapper, StoredValue, CmisExportContext> {

	private final CmisResultTransformer<QueryResult, ExportTarget> transformer = new CmisResultTransformer<QueryResult, ExportTarget>() {
		@Override
		public ExportTarget transform(QueryResult result) throws Exception {
			return newExportTarget(result);
		}
	};

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
		final int itemsPerPage = Math.max(10, cfg.getInteger(CmisSetting.EXPORT_PAGE_SIZE));
		final OperationContext ctx = session.createOperationContext();
		ctx.setLoadSecondaryTypeProperties(true);
		ctx.setFilterString("*");
		ctx.setMaxItemsPerPage(itemsPerPage);
		if (path != null) {
			final CmisObject obj;
			try {
				obj = session.getObjectByPath(path, ctx);
			} catch (CmisObjectNotFoundException e) {
				return null;
			}
			if (obj instanceof Folder) {
				return new Iterator<ExportTarget>() {
					private final Iterator<CmisObject> it = new CmisRecursiveIterator(session, Folder.class.cast(obj),
						true, ctx);

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
			final boolean searchAllVersions = session.getRepositoryInfo().getCapabilities()
				.isAllVersionsSearchableSupported();
			return new CmisPagingTransformerIterator<QueryResult, ExportTarget>(session.query(query, searchAllVersions,
				ctx), this.transformer);
		}
		return null;
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
	protected StoredValue getValue(StoredDataType type, Object value) {
		try {
			return new StoredValue(type, value);
		} catch (ParseException e) {
			throw new RuntimeException(String.format("Can't convert [%s] as a %s", value, type), e);
		}
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

	public static ExportEngine<?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(CmisCommon.TARGET_NAME);
	}

	@Override
	protected CmisObjectStorageTranslator getTranslator() {
		return new CmisObjectStorageTranslator();
	}

	@Override
	protected CmisExportDelegate<?> getExportDelegate(Session session, StoredObjectType type, String searchKey)
		throws Exception {
		CmisObject obj = session.getObject(searchKey);
		switch (type) {
			case ACL:
				return new CmisAclDelegate(this, new CmisAcl(decodeType(obj.getBaseType()), obj));
			case FOLDER:
				if (obj instanceof Folder) { return new CmisFolderDelegate(this, Folder.class.cast(obj)); }
				throw new ExportException(String.format("Object with ID [%s] (class %s) is not a Folder-type",
					searchKey, obj.getClass().getCanonicalName()));
			case DOCUMENT:
				if (obj instanceof Document) {
					// Is this the PWC? If so, then don't include it...
					Document doc = Document.class.cast(obj);
					if ((doc.isPrivateWorkingCopy() == Boolean.TRUE) || Tools.equals("pwc", doc.getVersionLabel())) {
						// We will not include the PWC in an export
						doc = doc.getObjectOfLatestVersion(false);
						if (doc == null) { return null; }
					}
					return new CmisDocumentDelegate(this, doc);
				}
				throw new ExportException(String.format("Object with ID [%s] (class %s) is not a Document-type",
					searchKey, obj.getClass().getCanonicalName()));
			case USER:
			case GROUP:
			default:
				break;
		}
		return null;
	}
}