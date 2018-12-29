package com.armedia.caliente.engine.cmis.exporter;

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.cmis.CmisCommon;
import com.armedia.caliente.engine.cmis.CmisPagingTransformerIterator;
import com.armedia.caliente.engine.cmis.CmisRecursiveIterator;
import com.armedia.caliente.engine.cmis.CmisResultTransformer;
import com.armedia.caliente.engine.cmis.CmisSessionFactory;
import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.cmis.CmisSetting;
import com.armedia.caliente.engine.cmis.CmisTransformerIterator;
import com.armedia.caliente.engine.cmis.CmisTranslator;
import com.armedia.caliente.engine.dynamic.mapper.AttributeMapper;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportEngine extends
	ExportEngine<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportContextFactory, CmisExportDelegateFactory> {

	private final CmisResultTransformer<QueryResult, ExportTarget> queryResultTransformer = new CmisResultTransformer<QueryResult, ExportTarget>() {
		@Override
		public ExportTarget transform(QueryResult result) throws Exception {
			return newExportTarget(result);
		}
	};

	private final CmisResultTransformer<CmisObject, ExportTarget> cmisObjectTransformer = new CmisResultTransformer<CmisObject, ExportTarget>() {
		@Override
		public ExportTarget transform(CmisObject result) throws Exception {
			return new ExportTarget(decodeType(result.getType()), result.getId(), result.getId());
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

	protected Iterator<ExportTarget> getPathIdIterator(final Session session, final CfgTools cfg) throws Exception {
		String id = cfg.getString(CmisSetting.EXPORT_ID);
		if (StringUtils.isEmpty(id)) {
			id = null;
		}

		String path = cfg.getString(CmisSetting.EXPORT_PATH);
		if (StringUtils.isEmpty(path)) {
			path = null;
		}

		if ((path == null) && (id == null)) { return null; }

		final CmisObject obj;
		if (id != null) {
			obj = session.getObject(id);
		} else {
			obj = session.getObjectByPath(path);
		}

		if (Folder.class.isInstance(obj)) {
			// This is a folder that we need to recurse into
			return new CmisTransformerIterator<>(new CmisRecursiveIterator(session, Folder.class.cast(obj), true),
				this.cmisObjectTransformer);
		}

		// Not a folder, so no need to recurse
		return Collections.singleton(new ExportTarget(decodeType(obj.getBaseType()), obj.getId(), obj.getId()))
			.iterator();
	}

	protected Iterator<ExportTarget> getQueryIterator(final Session session, final CfgTools cfg) throws Exception {
		final String query = cfg.getString(CmisSetting.EXPORT_QUERY);
		if (query == null) { return null; }
		final boolean searchAllVersions = session.getRepositoryInfo().getCapabilities()
			.isAllVersionsSearchableSupported();
		return new CmisPagingTransformerIterator<>(session.query(query, searchAllVersions),
			this.queryResultTransformer);
	}

	@Override
	protected void findExportResults(final Session session, CfgTools cfg, CmisExportDelegateFactory factory,
		TargetSubmitter submitter) throws Exception {
		Iterator<ExportTarget> it = null;

		// Is this a path or ID-based search?
		if (it == null) {
			it = getPathIdIterator(session, cfg);
		}

		// No search yet? Is this a query based search?
		if (it == null) {
			it = getQueryIterator(session, cfg);
		}

		// If we have no results, then we simply return...
		if (it == null) { return; }

		// If we have results, we submit them all
		while (it.hasNext()) {
			submitter.submit(it.next());
		}
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
	protected CmisExportContextFactory newContextFactory(Session session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Transformer transformer,
		AttributeMapper attributeMapper, Logger output, WarningTracker warningTracker) throws Exception {
		return new CmisExportContextFactory(this, session, cfg, objectStore, streamStore, output, warningTracker);
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