/**
 *
 */

package com.delta.cmsmf.datastore.cms;

import java.util.Collection;

import com.delta.cmsmf.datastore.cms.CmsAttributeHandlers.AttributeHandler;
import com.documentum.com.DfClientX;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

/**
 * @author diego
 *
 */
public class CmsType extends CmsObject<IDfType> {

	private static boolean HANDLERS_READY = false;

	private static synchronized void initHandlers() {
		if (CmsType.HANDLERS_READY) { return; }
		AttributeHandler handler = new AttributeHandler() {
			@Override
			public boolean includeInImport(IDfPersistentObject object, CmsAttribute attribute) throws DfException {
				return false;
			}
		};
		// These are the attributes that require special handling on import
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.ATTR_COUNT, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.ATTR_COUNT, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.START_POS, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING, CmsAttributes.NAME,
			handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.SUPER_NAME, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.ATTR_NAME, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.ATTR_TYPE, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.ATTR_LENGTH, handler);
		CmsAttributeHandlers.setAttributeHandler(CmsObjectType.ACL, CmsDataType.DF_STRING,
			CmsAttributes.ATTR_REPEATING, handler);

		CmsType.HANDLERS_READY = true;
	}

	public CmsType() {
		super(CmsObjectType.TYPE, IDfType.class);
		CmsType.initHandlers();
	}

	@Override
	protected boolean skipImport(IDfSession session) throws DfException {
		CmsAttribute typeNameAttr = getAttribute(CmsAttributes.NAME);
		String typeName = typeNameAttr.getValue().asString();
		return typeName.startsWith("dm_");
	}

	@Override
	protected void getDataProperties(Collection<CmsProperty> properties, IDfType user) throws DfException {
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

		IDfQuery dqlQry = new DfClientX().getQuery();
		dqlQry.setDQL(dql.toString());
		IDfCollection resultCol = dqlQry.execute(session, IDfQuery.DF_EXECREAD_QUERY);
		try {
			while (resultCol.next()) {
				IDfPersistentObject obj = session.getObject(resultCol.getId(CmsAttributes.NEW_OBJECT_ID));
				return castObject(obj);
			}
			// Nothing was created... we should explode
			throw new DfException(String.format("Failed to create the type [%s] with DQL: %s", typeName, dql));
		} finally {
			closeQuietly(resultCol);
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