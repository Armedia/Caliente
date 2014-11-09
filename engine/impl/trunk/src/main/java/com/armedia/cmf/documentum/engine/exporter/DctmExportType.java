/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.cmf.documentum.engine.DctmAttributeHandlers;
import com.armedia.cmf.documentum.engine.DctmAttributes;
import com.armedia.cmf.documentum.engine.DctmDataType;
import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DfUtils;
import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredObject;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportType extends DctmExportAbstract<IDfType> {

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (DctmExportType.HANDLERS_READY) { return; }
		// These are the attributes that require special handling on import
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING,
			DctmAttributes.ATTR_COUNT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING,
			DctmAttributes.ATTR_COUNT, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING, DctmAttributes.START_POS,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING, DctmAttributes.NAME,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING,
			DctmAttributes.SUPER_NAME, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING, DctmAttributes.ATTR_NAME,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING, DctmAttributes.ATTR_TYPE,
			DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING,
			DctmAttributes.ATTR_LENGTH, DctmAttributeHandlers.NO_IMPORT_HANDLER);
		DctmAttributeHandlers.setAttributeHandler(DctmObjectType.ACL, DctmDataType.DF_STRING,
			DctmAttributes.ATTR_REPEATING, DctmAttributeHandlers.NO_IMPORT_HANDLER);

		DctmExportType.HANDLERS_READY = true;
	}

	private static boolean SPECIAL_TYPES_READY = false;
	private static Set<String> SPECIAL_TYPES = Collections.emptySet();

	private static synchronized void initSpecialTypes() {
		if (DctmExportType.SPECIAL_TYPES_READY) { return; }
		String specialTypes = "";
		// TODO: Setting.SPECIAL_TYPES.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(specialTypes);
		DctmExportType.SPECIAL_TYPES = Collections.unmodifiableSet(new HashSet<String>(strTokenizer.getTokenList()));
		DctmExportType.SPECIAL_TYPES_READY = true;
	}

	public static boolean isSpecialType(String type) {
		DctmExportType.initSpecialTypes();
		return DctmExportType.SPECIAL_TYPES.contains(type);
	}

	protected DctmExportType(DctmExportEngine engine) {
		super(engine, DctmObjectType.TYPE);
		DctmExportType.initHandlers();
		DctmExportType.initSpecialTypes();
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfType type) throws DfException {
		String superName = type.getSuperName();
		if ((superName != null) && (superName.length() > 0)) {
			superName = String.format(" (extends %s)", superName);
		} else {
			superName = "";
		}
		return String.format("%s%s", type.getName(), superName);
	}

	@Override
	protected String calculateBatchId(IDfSession session, IDfType type) throws DfException {
		// Calculate the maximum depth that this folder resides in, from its parents.
		// Keep track of visited nodes, and explode on a loop.
		Set<String> visited = new LinkedHashSet<String>();
		int depth = calculateDepth(session, type.getName(), visited);
		// We return it in zero-padded hex to allow for large numbers (up to 2^64
		// depth), and also maintain consistent sorting
		return String.format("%016x", depth);
	}

	private int calculateDepth(IDfSession session, String typeName, Set<String> visited) throws DfException {
		// If the folder has already been visited, we have a loop...so let's explode loudly
		if (visited.contains(typeName)) { throw new DfException(String.format(
			"Type loop detected, element [%s] exists twice: %s", typeName, visited.toString())); }
		visited.add(typeName);
		try {
			int depth = 0;
			String dql = String.format("select super_name from dm_type where name = '%s'", typeName);
			IDfCollection results = DfUtils.executeQuery(session, dql, IDfQuery.DF_EXECREAD_QUERY);
			try {
				while (results.next()) {
					// My depth is the maximum depth from any of my parents, plus one
					String superName = results.getString(DctmAttributes.SUPER_NAME);
					if (results.isNull(DctmAttributes.SUPER_NAME) || StringUtils.isBlank(superName)) {
						continue;
					}
					depth = Math.max(depth, calculateDepth(session, superName, visited) + 1);
				}
			} finally {
				DfUtils.closeQuietly(results);
			}
			return depth;
		} finally {
			visited.remove(typeName);
		}
	}

	@Override
	protected Collection<IDfPersistentObject> findRequirements(IDfSession session, StoredObject<IDfValue> marshaled,
		IDfType type, ExportContext<IDfSession, IDfPersistentObject, IDfValue> ctx) throws Exception {
		Collection<IDfPersistentObject> ret = super.findRequirements(session, marshaled, type, ctx);
		IDfType superType = type.getSuperType();
		if (superType != null) {
			if (DctmExportType.isSpecialType(superType.getName())) {
				this.log.warn(String.format("Will not export special type [%s] (supertype of [%s])",
					superType.getName(), type.getName()));
			} else {
				ret.add(superType);
			}
		}
		return ret;
	}
}