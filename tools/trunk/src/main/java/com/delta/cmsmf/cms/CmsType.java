/**
 *
 */

package com.delta.cmsmf.cms;

import com.delta.cmsmf.exception.CMSMFException;
import com.delta.cmsmf.utils.DfUtils;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class CmsType extends CmsObject<IDfType> {

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsType.HANDLERS_READY) { return; }
		// These are the attributes that require special handling on import
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.ATTR_COUNT,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.ATTR_COUNT,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.START_POS,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.SUPER_NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.ATTR_NAME,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.ATTR_TYPE,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.ATTR_LENGTH,
			CmsAttributeHandlers.NO_IMPORT_HANDLER);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.ATTR_REPEATING, CmsAttributeHandlers.NO_IMPORT_HANDLER);

		CmsType.HANDLERS_READY = true;
	}

	public CmsType() {
		super(CmsObjectType.TYPE, IDfType.class);
		CmsType.initHandlers();
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

	@Override
	protected boolean skipImport(IDfSession session) throws DfException {
		CmsAttribute typeNameAttr = getAttribute(CmsAttributes.NAME);
		String typeName = typeNameAttr.getValue().asString();
		return typeName.startsWith("dm_");
	}

	@Override
	protected boolean isValidForLoad(IDfType type) throws DfException {
		final String name = type.getName();
		return !name.startsWith("dm_");
	}

	@Override
	protected void doPersistDependencies(IDfType type, CmsDependencyManager manager) throws DfException, CMSMFException {
		IDfType superType = type.getSuperType();
		if (superType == null) { return; }
		// TODO: Ignore system types?
		if (superType.getName().startsWith("dm_")) { return; }
		manager.persistDependency(superType);
	}

	@Override
	protected IDfType newObject(IDfSession session) throws DfException {
		String typeName = getAttribute(CmsAttributes.NAME).getValue().asString();
		String superTypeName = getAttribute(CmsAttributes.SUPER_NAME).getValue().asString();

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

		int attrCount = getAttribute(CmsAttributes.ATTR_COUNT).getValue().asInteger();
		int startPosition = getAttribute(CmsAttributes.START_POS).getValue().asInteger();
		CmsAttribute attrNames = getAttribute(CmsAttributes.ATTR_NAME);
		CmsAttribute attrTypes = getAttribute(CmsAttributes.ATTR_TYPE);
		CmsAttribute attrLengths = getAttribute(CmsAttributes.ATTR_LENGTH);
		CmsAttribute attrRepeating = getAttribute(CmsAttributes.ATTR_REPEATING);

		// Start the DQL
		StringBuilder dql = new StringBuilder();
		dql.append("Create Type \"").append(typeName).append("\"( ");
		// Iterate through only the custom attributes of the type object and add them to the dql
		// string
		for (int iIndex = startPosition; iIndex < attrCount; ++iIndex) {
			String attrName = attrNames.getValue(iIndex).asString();
			dql.append(attrName).append(" ");
			int attrType = attrTypes.getValue(iIndex).asInteger();
			switch (attrType) {
				case IDfAttr.DM_BOOLEAN:
					dql.append("boolean");
					break;
				case IDfAttr.DM_INTEGER:
					dql.append("Integer");
					break;
				case IDfAttr.DM_STRING:
					int attrLength = attrLengths.getValue(iIndex).asInteger();
					dql.append("String(").append(attrLength).append(")");
					break;
				case IDfAttr.DM_ID:
					dql.append("ID");
					break;
				case IDfAttr.DM_TIME:
					dql.append("Date");
					break;
				case IDfAttr.DM_DOUBLE:
					dql.append("double");
					break;
				case IDfAttr.DM_UNDEFINED:
					dql.append("Undefined");
					break;
				default:
					break;
			}
			boolean isRepeating = attrRepeating.getValue(iIndex).asBoolean();
			if (isRepeating) {
				dql.append(" Repeating");
			}

			if (iIndex != (attrCount - 1)) {
				dql.append(", ");
			}
		}

		// Add the supertype phrase if needed
		dql.append(") With SuperType ").append((superType != null) ? superTypeName : "Null ").append(" Publish");

		IDfCollection resultCol = DfUtils.executeQuery(session, dql.toString(), IDfQuery.DF_QUERY);
		try {
			while (resultCol.next()) {
				IDfPersistentObject obj = session.getObject(resultCol.getId(CmsAttributes.NEW_OBJECT_ID));
				return castObject(obj);
			}
			// Nothing was created... we should explode
			throw new DfException(String.format("Failed to create the type [%s] with DQL: %s", typeName, dql));
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	@Override
	protected void prepareForConstruction(IDfType object, boolean newObject) throws DfException {
	}

	@Override
	protected IDfType locateInCms(IDfSession session) throws DfException {
		return session.getType(getAttribute(CmsAttributes.NAME).getValue().asString());
	}
}