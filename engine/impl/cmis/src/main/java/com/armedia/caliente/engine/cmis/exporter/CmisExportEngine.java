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
package com.armedia.caliente.engine.cmis.exporter;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import com.armedia.caliente.engine.cmis.CmisSessionFactory;
import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.cmis.CmisTransformerIterator;
import com.armedia.caliente.engine.cmis.CmisTranslator;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.exporter.ExportEngine;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.exporter.ExportSetting;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObject.Archetype;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.CmfCrypt;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.StreamTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportEngine extends
	ExportEngine<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportContextFactory, CmisExportDelegateFactory, CmisExportEngineFactory> {

	private final class QueryResultTransformer {
		private final Session session;
		private final Map<String, ObjectType> typeCache = new HashMap<>();

		private QueryResultTransformer(Session session) {
			this.session = session;
		}

		public ExportTarget transform(QueryResult r) throws Exception {
			PropertyData<?> objectId = r.getPropertyById(PropertyIds.OBJECT_ID);
			if (objectId == null) {
				CmisExportEngine.this.log
					.warn("UNSUPPORTED OBJECT: Failed to find the cmis:objectId property as part of the query result");
				return null;
			}

			CmfObject.Archetype type = null;
			PropertyData<?>[] objectTypes = {
				r.getPropertyById(PropertyIds.OBJECT_TYPE_ID), r.getPropertyById(PropertyIds.BASE_TYPE_ID)
			};

			for (PropertyData<?> t : objectTypes) {
				if (t == null) {
					continue;
				}

				Object firstValue = t.getFirstValue();
				if (CmisExportEngine.this.log.isTraceEnabled()) {
					CmisExportEngine.this.log.trace("Found property [{}] with value [{}]", t.getId(), firstValue);
				}
				String value = Tools.toString(firstValue);
				if (StringUtils.isNotBlank(value)) {
					// Use the cache ...
					type = decodeType(this.typeCache.computeIfAbsent(value, this.session::getTypeDefinition));
				} else {
					type = decodeType(value);
				}
				if (type != null) {
					CmisExportEngine.this.log.trace("Object type [{}] decoded as [{}]", firstValue, type);
					break;
				}
			}
			String id = Tools.toString(objectId.getFirstValue());
			if (type == null) {
				CmisExportEngine.this.log.warn(
					"UNSUPPORTED OBJECT: Failed to find the cmis:objectTypeId or the cmis:baseTypeId properties as part of the query result for object with ID [{}], or the returned value couldn't be decoded",
					id);
				return null;
			}
			return ExportTarget.from(type, id, id);
		}
	}

	private final ExportTarget cmisObjectToExportTarget(CmisObject result) throws Exception {
		ObjectType type = result.getType();
		Archetype archetype = decodeType(type);
		if (archetype == null) {
			// TODO: Is it, perhaps, a reference? How to find out?
			this.log.warn(
				"UNSUPPORTED OBJECT: Failed to decode the ArcheType for result [{}] (name={}, type={}, baseType={})",
				result.getId(), result.getName(), type.getId(), result.getBaseTypeId().value());
			return null;
		}
		return ExportTarget.from(archetype, result.getId(), result.getId());
	}

	public CmisExportEngine(CmisExportEngineFactory factory, Logger output, WarningTracker warningTracker,
		File baseData, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> contentStore, CfgTools settings) {
		super(factory, output, warningTracker, baseData, objectStore, contentStore, settings, true);
	}

	protected Iterator<ExportTarget> getPathIterator(final Session session, String path, boolean excludeEmptyFolders)
		throws Exception {
		final CmisObject obj;
		try {
			obj = session.getObjectByPath(path);
		} catch (CmisObjectNotFoundException e) {
			this.log.warn("No object found at [{}]", path);
			return null;
		}
		if (Folder.class.isInstance(obj)) {
			// This is a folder that we need to recurse into
			return new CmisTransformerIterator<>(
				new CmisRecursiveIterator(session, Folder.class.cast(obj), excludeEmptyFolders),
				this::cmisObjectToExportTarget);
		}

		// Not a folder, so no need to recurse
		Archetype type = decodeType(obj.getBaseType());
		if (type == null) {
			this.log.warn(
				"UNSUPPORTED OBJECT: The object at path [{}](id={}) is not of a supported type (type={}, baseType={})",
				path, obj.getId(), obj.getType().getId(), obj.getBaseTypeId().value());
			return Collections.emptyListIterator();
		}
		return Collections.singleton(cmisObjectToExportTarget(obj)).iterator();
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByPath(Session session, CfgTools configuration, String path)
		throws Exception {
		return StreamTools
			.of(getPathIterator(session, path, configuration.getBoolean(ExportSetting.IGNORE_EMPTY_FOLDERS)));
	}

	protected Iterator<ExportTarget> getQueryIterator(final Session session, final String query) throws Exception {
		if (StringUtils.isBlank(query)) { return null; }
		final boolean searchAllVersions = session.getRepositoryInfo().getCapabilities()
			.isAllVersionsSearchableSupported();
		return new CmisPagingTransformerIterator<>(session.query(query, searchAllVersions),
			new QueryResultTransformer(session)::transform);
	}

	@Override
	protected String findFolderName(Session session, String folderId, Object ecmObject) {
		try {
			CmisObject folder = session.getObject(folderId);
			folder.refresh();
			return folder.getName();
		} catch (CmisObjectNotFoundException e) {
			return null;
		}
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsByQuery(Session session, CfgTools configuration, String query)
		throws Exception {
		return StreamTools.of(getQueryIterator(session, query));
	}

	@Override
	protected Stream<ExportTarget> findExportTargetsBySearchKey(Session session, CfgTools configuration,
		String searchKey) throws Exception {
		try {
			CmisObject obj = session.getObject(searchKey);
			CmfObject.Archetype type = decodeType(obj.getBaseType());
			if (type == null) {
				this.log.warn(
					"UNSUPPORTED OBJECT: the object located with search key [{}] is not of a supported type (name={}, type={}, baseType={})",
					searchKey, obj.getName(), obj.getType().getId(), obj.getBaseTypeId().value());
				return Stream.empty();
			}

			// Not a folder, so no recursion
			if (type != CmfObject.Archetype.FOLDER) {
				return Stream.of(ExportTarget.from(type, obj.getId(), searchKey));
			}

			// RECURSE!!!
			return findExportTargetsByPath(session, configuration, Folder.class.cast(obj).getPath());
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
	protected CmisExportContextFactory newContextFactory(Session session, CfgTools cfg, CmfObjectStore<?> objectStore,
		CmfContentStore<?, ?> streamStore, Transformer transformer, Logger output, WarningTracker warningTracker)
		throws Exception {
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