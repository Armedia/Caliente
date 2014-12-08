/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;

import com.armedia.commons.utilities.Tools;
import com.delta.cmsmf.cfg.Setting;
import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CmsType extends CmsObject<IDfType> {

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsType.HANDLERS_READY) { return; }
		// These are the attributes that require special handling on import
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.TYPE, CmsDataType.DF_STRING, CmsAttributes.ATTR_COUNT,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.TYPE, CmsDataType.DF_STRING, CmsAttributes.ATTR_COUNT,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.TYPE, CmsDataType.DF_STRING, CmsAttributes.START_POS,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.TYPE, CmsDataType.DF_STRING, CmsAttributes.NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.TYPE, CmsDataType.DF_STRING, CmsAttributes.SUPER_NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.TYPE, CmsDataType.DF_STRING, CmsAttributes.ATTR_NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.TYPE, CmsDataType.DF_STRING, CmsAttributes.ATTR_TYPE,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.TYPE, CmsDataType.DF_STRING, CmsAttributes.ATTR_LENGTH,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.TYPE, CmsDataType.DF_STRING,
			CmsAttributes.ATTR_REPEATING, CmsAttributeHandlers.NO_IMPORT_HANDLER);

		CmsType.HANDLERS_READY = true;
	}

	private static boolean SPECIAL_TYPES_READY = false;
	private static Set<String> SPECIAL_TYPES = Collections.emptySet();

	private static synchronized void initSpecialTypes() {
		if (CmsType.SPECIAL_TYPES_READY) { return; }
		String specialTypes = Setting.SPECIAL_TYPES.getString();
		StrTokenizer strTokenizer = StrTokenizer.getCSVInstance(specialTypes);
		CmsType.SPECIAL_TYPES = Collections.unmodifiableSet(new HashSet<String>(strTokenizer.getTokenList()));
		CmsType.SPECIAL_TYPES_READY = true;
	}

	public static boolean isSpecialType(String type) {
		CmsType.initSpecialTypes();
		return CmsType.SPECIAL_TYPES.contains(type);
	}

	public CmsType() {
		super(IDfType.class);
		CmsType.initHandlers();
		CmsType.initSpecialTypes();
	}

	@Override
	protected String calculateLabel(IDfType type) throws DfException {
		String superName = type.getSuperName();
		if ((superName != null) && (superName.length() > 0)) {
			superName = String.format(" (extends %s)", superName);
		} else {
			superName = "";
		}
		return String.format("%s%s", type.getName(), superName);
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
					String superName = results.getString(CmsAttributes.SUPER_NAME);
					if (results.isNull(CmsAttributes.SUPER_NAME) || StringUtils.isBlank(superName)) {
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
	protected String calculateBatchId(IDfType type) throws DfException {
		// Calculate the maximum depth that this folder resides in, from its parents.
		// Keep track of visited nodes, and explode on a loop.
		Set<String> visited = new LinkedHashSet<String>();
		int depth = calculateDepth(type.getSession(), type.getName(), visited);
		// We return it in zero-padded hex to allow for large numbers (up to 2^64
		// depth), and also maintain consistent sorting
		return String.format("%016x", depth);
	}

	@Override
	protected void doPersistRequirements(IDfType type, CmsTransferContext ctx, CmsDependencyManager manager)
		throws DfException, CMSMFException {
		IDfType superType = type.getSuperType();
		if (superType == null) { return; }
		if (CmsType.isSpecialType(superType.getName())) {
			this.log.warn(String.format("Will not export special type [%s] (supertype of [%s])", superType.getName(),
				type.getName()));
			return;
		}
		manager.persistRelatedObject(superType);
	}

	@Override
	protected IDfType newObject(CmsTransferContext ctx) throws DfException {
		String typeName = getAttribute(CmsAttributes.NAME).getValue().asString();
		String superTypeName = getAttribute(CmsAttributes.SUPER_NAME).getValue().asString();
		final IDfSession session = ctx.getSession();
		// TODO: Ensure the supertype is there
		IDfType superType = null;
		if (superTypeName.length() > 0) {
			superType = session.getType(superTypeName);
			if (superType == null) {
				// We require a supertype that doesn't exist
				throw new DfException(String.format(
					"Attempting to create type [%s] before its supertype [%s] is created", typeName, superTypeName));
			}
		}

		final int attrCount = getAttribute(CmsAttributes.ATTR_COUNT).getValue().asInteger();
		final int startPosition = getAttribute(CmsAttributes.START_POS).getValue().asInteger();
		final CmsAttribute attrNames = getAttribute(CmsAttributes.ATTR_NAME);
		final CmsAttribute attrTypes = getAttribute(CmsAttributes.ATTR_TYPE);
		final CmsAttribute attrLengths = getAttribute(CmsAttributes.ATTR_LENGTH);
		final CmsAttribute attrRepeating = getAttribute(CmsAttributes.ATTR_REPEATING);

		// Start the DQL
		final StringBuilder dql = new StringBuilder();
		dql.append("create type \"").append(typeName).append("\"( ");
		// Iterate through only the custom attributes of the type object and add them to the dql
		// string
		for (int i = startPosition; i < attrCount; ++i) {
			// If we're not the first, we need a comma
			if (i > startPosition) {
				dql.append(", ");
			}
			dql.append(attrNames.getValue(i).asString()).append(" ");
			switch (attrTypes.getValue(i).asInteger()) {
				case IDfAttr.DM_BOOLEAN:
					dql.append("boolean");
					break;
				case IDfAttr.DM_INTEGER:
					dql.append("integer");
					break;
				case IDfAttr.DM_STRING:
					int attrLength = attrLengths.getValue(i).asInteger();
					dql.append("string(").append(attrLength).append(")");
					break;
				case IDfAttr.DM_ID:
					dql.append("id");
					break;
				case IDfAttr.DM_TIME:
					dql.append("date");
					break;
				case IDfAttr.DM_DOUBLE:
					dql.append("double");
					break;
				case IDfAttr.DM_UNDEFINED:
					dql.append("undefined");
					break;
				default:
					break;
			}
			boolean isRepeating = attrRepeating.getValue(i).asBoolean();
			if (isRepeating) {
				dql.append(" repeating");
			}
		}

		// Add the supertype phrase if needed
		dql.append(") with supertype ").append((superType != null) ? superTypeName : "null").append(" publish");

		if (this.log.isInfoEnabled()) {
			this.log.info(String.format("Creating new type [%s] with DQL:%n%n%s%n", typeName, dql));
		}

		IDfCollection resultCol = DfUtils.executeQuery(session, dql.toString(), IDfQuery.DF_QUERY);
		try {
			while (resultCol.next()) {
				return castObject(session.getObject(resultCol.getId(CmsAttributes.NEW_OBJECT_ID)));
			}
			// Nothing was created... we should explode
			throw new DfException(String.format("Failed to create the type [%s] with DQL: %s", typeName, dql));
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	@Override
	protected IDfId persistChanges(IDfType object, CmsTransferContext context) throws DfException, CMSMFException {
		// In particular, we don't persist changes to types
		return object.getObjectId();
	}

	@Override
	protected boolean isShortConstructionCycle() {
		// Types require the short construction cycle
		return true;
	}

	@Override
	protected boolean isSameObject(IDfType object) throws DfException {
		// It's "impossible" to compare types...rather - it's far too complicated to do
		// anything if the type is already there...so we just check names...
		return Tools.equals(object.getName(), getAttribute(CmsAttributes.NAME).getValue().asString());
	}

	@Override
	protected void prepareForConstruction(IDfType object, boolean newObject, CmsTransferContext context)
		throws DfException {
	}

	@Override
	protected boolean isValidForLoad(IDfType type) throws DfException {
		if (CmsType.isSpecialType(type.getName())) { return false; }
		// If the type name is the same as dmi_${objectId}, we skip it
		if (Tools.equals(type.getName(), String.format("dmi_%s", type.getObjectId().getId()))) { return false; }
		return super.isValidForLoad(type);
	}

	@Override
	protected boolean skipImport(CmsTransferContext ctx) throws DfException {
		IDfValue typeNameValue = getAttribute(CmsAttributes.NAME).getValue();
		final String typeName = typeNameValue.asString();
		if (CmsType.isSpecialType(typeName)) {
			this.log.warn(String.format("Will not import special type [%s]", typeName));
			return true;
		}
		// If the type name is the same as dmi_${objectId}, we skip it
		if (Tools.equals(typeName, String.format("dmi_%s", getId()))) { return false; }
		return super.skipImport(ctx);
	}

	@Override
	protected IDfType locateInCms(CmsTransferContext ctx) throws DfException {
		return ctx.getSession().getType(getAttribute(CmsAttributes.NAME).getValue().asString());
	}
}