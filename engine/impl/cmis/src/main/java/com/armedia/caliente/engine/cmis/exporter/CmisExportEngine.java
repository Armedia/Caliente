package com.armedia.caliente.engine.cmis.exporter;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

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
import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.cmis.CmisPagingTransformerIterator;
import com.armedia.caliente.engine.cmis.CmisRecursiveIterator;
import com.armedia.caliente.engine.cmis.CmisResultTransformer;
import com.armedia.caliente.engine.cmis.CmisSessionFactory;
import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.cmis.CmisTransformerIterator;
import com.armedia.caliente.engine.cmis.CmisTranslator;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.StreamTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportEngine extends
	ExportEngine<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportContextFactory, CmisExportDelegateFactory, CmisExportEngineFactory> {

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

	public CmisExportEngine(CmisExportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, true);
	}

	protected ExportTarget newExportTarget(QueryResult r) throws ExportException {
		PropertyData<?> objectId = r.getPropertyById(PropertyIds.OBJECT_ID);
		if (objectId == null) {
			throw new ExportException("Failed to find the cmis:objectId property as part of the query result");
		}

		CmfObject.Archetype type = null;
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
		if (type == null) {
			throw new ExportException(
				"Failed to find the cmis:objectTypeId or the cmis:baseTypeId properties as part of the query result, or the returned value couldn't be decoded");
		}
		String id = Tools.toString(objectId.getFirstValue());
		return new ExportTarget(type, id, id);
	}

	protected Iterator<ExportTarget> getPathIterator(final Session session, String path) throws Exception {
		final CmisObject obj;
		try {
			obj = session.getObjectByPath(path);
		} catch (CmisObjectNotFoundException e) {
			this.log.warn("No object found at [{}]", path);
			return null;
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

	@Override
	protected Stream<ExportTarget> findExportTargetsByPath(Session session, CfgTools configuration,
		CmisExportDelegateFactory factory, String path) throws Exception {
		return StreamTools.of(getPathIterator(session, path));
	}

	protected Iterator<ExportTarget> getQueryIterator(final Session session, final String query) throws Exception {
		if (StringUtils.isBlank(query)) { return null; }
		final boolean searchAllVersions = session.getRepositoryInfo().getCapabilities()
			.isAllVersionsSearchableSupported();
		return new CmisPagingTransformerIterator<>(session.query(query, searchAllVersions),
			this.queryResultTransformer);
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByQuery(Session session, CfgTools configuration,
		CmisExportDelegateFactory factory, String query) throws Exception {
		return StreamTools.of(getQueryIterator(session, query));
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsBySearchKey(Session session, CfgTools configuration,
		CmisExportDelegateFactory factory, String searchKey) throws Exception {
		try {
			CmisObject obj = session.getObject(searchKey);
			CmfObject.Archetype type = decodeType(obj.getBaseType());

			// Not a folder, so no recursion
			if (type != CmfObject.Archetype.FOLDER) { return Stream.of(new ExportTarget(type, obj.getId(), searchKey)); }

			// RECURSE!!!
			return findExportTargetsByPath(session, configuration, factory, Folder.class.cast(obj).getPath());
		} catch (CmisObjectNotFoundException e) {
			return null;
		}
	}

	protected CmfObject.Archetype decodeType(ObjectType type) throws ExportException {
		if (!type.isBaseType()) { return decodeType(type.getBaseType()); }
		return decodeType(type.getId());
	}

	protected CmfObject.Archetype decodeType(String type) throws ExportException {
		final BaseTypeId id;
		try {
			id = BaseTypeId.fromValue(type);
		} catch (IllegalArgumentException e) {
			throw new ExportException(String.format("Unknown base type [%s]", type), e);
		}

		switch (id) {
			case CMIS_DOCUMENT:
				return CmfObject.Archetype.DOCUMENT;
			case CMIS_FOLDER:
				return CmfObject.Archetype.FOLDER;
			default:
				return null;
		}
	}

	@Override
	protected CmfValue getValue(CmfValue.Type type, Object value) {
		return CmfValue.newValue(type, value);
	}

	@Override
	protected CmisSessionFactory newSessionFactory(CfgTools cfg, CmfCrypt crypto) throws Exception {
		return new CmisSessionFactory(cfg, crypto);
	}

	@Override
	protected CmisExportContextFactory newContextFactory(Session session, CfgTools cfg,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, Transformer transformer, Logger output,
		WarningTracker warningTracker) throws Exception {
		return new CmisExportContextFactory(this, session, cfg, objectStore, streamStore, output, warningTracker);
	}

	@Override
	protected CmisExportDelegateFactory newDelegateFactory(Session session, CfgTools cfg) throws Exception {
		return new CmisExportDelegateFactory(this, cfg);
	}

	@Override
	protected CmisTranslator getTranslator() {
		return new CmisTranslator();
	}
}