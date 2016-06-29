package com.armedia.cmf.engine.cmis.exporter;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.CmfCrypt;
import com.armedia.cmf.engine.cmis.CmisCommon;
import com.armedia.cmf.engine.cmis.CmisPagingTransformerIterator;
import com.armedia.cmf.engine.cmis.CmisRecursiveIterator;
import com.armedia.cmf.engine.cmis.CmisResultTransformer;
import com.armedia.cmf.engine.cmis.CmisSessionFactory;
import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.cmis.CmisSetting;
import com.armedia.cmf.engine.cmis.CmisTranslator;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportEngine extends
	ExportEngine<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportContextFactory, CmisExportDelegateFactory> {

	private final CmisResultTransformer<QueryResult, ExportTarget> transformer = new CmisResultTransformer<QueryResult, ExportTarget>() {
		@Override
		public ExportTarget transform(QueryResult result) throws Exception {
			return newExportTarget(result);
		}
	};

	public CmisExportEngine() {
		super(new CmfCrypt());
	}

	protected ExportTarget newExportTarget(QueryResult r) throws ExportException {
		PropertyData<?> objectId = r.getPropertyById(PropertyIds.OBJECT_ID);
		if (objectId == null) { throw new ExportException(
			"Failed to find the cmis:objectId property as part of the query result"); }

		CmfType type = null;
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
	protected Iterator<ExportTarget> findExportResults(final Session session, CfgTools cfg,
		CmisExportDelegateFactory factory) throws Exception {
		String path = cfg.getString(CmisSetting.EXPORT_PATH);
		if (StringUtils.isEmpty(path)) {
			path = null;
		}
		String id = cfg.getString(CmisSetting.EXPORT_ID);
		if (StringUtils.isEmpty(id)) {
			id = null;
		}
		if ((path != null) || (id != null)) {
			final CmisObject obj;
			if (id != null) {
				try {
					obj = session.getObject(id);
				} catch (CmisObjectNotFoundException e) {
					return null;
				}
			} else if (path != null) {
				try {
					obj = session.getObjectByPath(path);
				} catch (CmisObjectNotFoundException e) {
					return null;
				}
			} else {
				throw new ExportException("Both the path and ID specifications were empty or null strings");
			}

			if (Folder.class.isInstance(obj)) {
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
							throw new RuntimeException(
								String.format("Failed to decode the object type [%s] for object [%s]",
									next.getType().getId(), next.getId()),
								e);
						}
					}

					@Override
					public void remove() {
						this.it.remove();
					}
				};
			} else {
				try {
					return Collections
						.singleton(new ExportTarget(decodeType(obj.getBaseType()), obj.getId(), obj.getId()))
						.iterator();
				} catch (CmisObjectNotFoundException e) {
					return null;
				}
			}
		}

		final String query = cfg.getString(CmisSetting.EXPORT_QUERY);
		if (query != null) {
			final boolean searchAllVersions = session.getRepositoryInfo().getCapabilities()
				.isAllVersionsSearchableSupported();
			return new CmisPagingTransformerIterator<QueryResult, ExportTarget>(session.query(query, searchAllVersions),
				this.transformer);
		}
		return null;
	}

	protected CmfType decodeType(ObjectType type) throws ExportException {
		if (!type.isBaseType()) { return decodeType(type.getBaseType()); }
		return decodeType(type.getId());
	}

	protected CmfType decodeType(String type) throws ExportException {
		final BaseTypeId id;
		try {
			id = BaseTypeId.fromValue(type);
		} catch (IllegalArgumentException e) {
			throw new ExportException(String.format("Unknown base type [%s]", type), e);
		}

		switch (id) {
			case CMIS_DOCUMENT:
				return CmfType.DOCUMENT;
			case CMIS_FOLDER:
				return CmfType.FOLDER;
			default:
				return null;
		}
	}

	@Override
	protected CmfValue getValue(CmfDataType type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmisSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new CmisSessionFactory(cfg, crypto);
	}

	@Override
	protected CmisExportContextFactory newContextFactory(Session session, CfgTools cfg) throws Exception {
		return new CmisExportContextFactory(this, session, cfg);
	}

	@Override
	protected CmisExportDelegateFactory newDelegateFactory(Session session, CfgTools cfg) throws Exception {
		return new CmisExportDelegateFactory(this, cfg);
	}

	@Override
	protected Set<String> getTargetNames() {
		return CmisCommon.TARGETS;
	}

	public static ExportEngine<?, ?, ?, ?, ?, ?> getExportEngine() {
		return ExportEngine.getExportEngine(CmisCommon.TARGET_NAME);
	}

	@Override
	protected CmisTranslator getTranslator() {
		return new CmisTranslator();
	}
}