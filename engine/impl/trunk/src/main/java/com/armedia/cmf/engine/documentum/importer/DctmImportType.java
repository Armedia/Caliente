/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import com.armedia.cmf.engine.documentum.DctmAttributes;
import com.armedia.cmf.engine.documentum.DctmObjectType;
import com.armedia.cmf.engine.documentum.DfUtils;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.commons.utilities.Tools;
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
public class DctmImportType extends DctmImportDelegate<IDfType> {

	DctmImportType(DctmImportDelegateFactory factory, StoredObject<IDfValue> storedObject) throws Exception {
		super(factory, IDfType.class, DctmObjectType.TYPE, storedObject);
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
	protected IDfType newObject(DctmImportContext ctx) throws DfException {
		String typeName = this.storedObject.getAttribute(DctmAttributes.NAME).getValue().asString();
		String superTypeName = this.storedObject.getAttribute(DctmAttributes.SUPER_NAME).getValue().asString();
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

		final int attrCount = this.storedObject.getAttribute(DctmAttributes.ATTR_COUNT).getValue().asInteger();
		final int startPosition = this.storedObject.getAttribute(DctmAttributes.START_POS).getValue().asInteger();
		final StoredAttribute<IDfValue> attrNames = this.storedObject.getAttribute(DctmAttributes.ATTR_NAME);
		final StoredAttribute<IDfValue> attrTypes = this.storedObject.getAttribute(DctmAttributes.ATTR_TYPE);
		final StoredAttribute<IDfValue> attrLengths = this.storedObject.getAttribute(DctmAttributes.ATTR_LENGTH);
		final StoredAttribute<IDfValue> attrRepeating = this.storedObject.getAttribute(DctmAttributes.ATTR_REPEATING);

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
				return castObject(session.getObject(resultCol.getId(DctmAttributes.NEW_OBJECT_ID)));
			}
			// Nothing was created... we should explode
			throw new DfException(String.format("Failed to create the type [%s] with DQL: %s", typeName, dql));
		} finally {
			DfUtils.closeQuietly(resultCol);
		}
	}

	@Override
	protected IDfId persistChanges(IDfType object, DctmImportContext context) throws DfException, ImportException {
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
		return Tools
			.equals(object.getName(), this.storedObject.getAttribute(DctmAttributes.NAME).getValue().asString());
	}

	@Override
	protected void prepareForConstruction(IDfType object, boolean newObject, DctmImportContext context)
		throws DfException {
	}

	@Override
	protected boolean skipImport(DctmImportContext ctx) throws DfException {
		IDfValue typeNameValue = this.storedObject.getAttribute(DctmAttributes.NAME).getValue();
		final String typeName = typeNameValue.asString();
		if (ctx.isSpecialType(typeName)) {
			this.log.warn(String.format("Will not import special type [%s]", typeName));
			return true;
		}
		// If the type name is the same as dmi_${objectId}, we skip it
		if (Tools.equals(typeName, String.format("dmi_%s", this.storedObject.getId()))) { return false; }
		return super.skipImport(ctx);
	}

	@Override
	protected IDfType locateInCms(DctmImportContext ctx) throws DfException {
		return ctx.getSession().getType(this.storedObject.getAttribute(DctmAttributes.NAME).getValue().asString());
	}
}