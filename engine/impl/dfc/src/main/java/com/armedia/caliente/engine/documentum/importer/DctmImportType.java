/**
 *
 */

package com.armedia.caliente.engine.documentum.importer;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.documentum.DctmAttributes;
import com.armedia.caliente.engine.documentum.DctmObjectType;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeMapper.Mapping;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.dfc.util.DfUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfObjectNotFoundException;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfQuery;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.content.IDfStore;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmImportType extends DctmImportDelegate<IDfType> {

	DctmImportType(DctmImportDelegateFactory factory, CmfObject<IDfValue> storedObject) throws Exception {
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

	protected void setDefaultACL(DctmImportContext ctx, IDfType type) throws DfException {
		CmfProperty<IDfValue> prop = this.cmfObject.getProperty(IntermediateProperty.DEFAULT_ACL);
		if ((prop == null) || !prop.hasValues()) { return; }

		// Step 1: is the ACL there?
		IDfId aclId = prop.getValue().asId();
		Mapping m = ctx.getAttributeMapper().getTargetMapping(CmfType.ACL, DctmAttributes.R_OBJECT_ID, aclId.getId());
		if (m == null) {
			// The mapping isn't there...we can't set it...
			this.log.warn("The ACL with ID[{}], specified as default for type [{}] was not imported", aclId.getId(),
				type.getName());
			return;
		}

		final IDfSession session = ctx.getSession();
		final IDfACL acl;
		try {
			acl = IDfACL.class.cast(session.getObject(aclId));
		} catch (DfObjectNotFoundException e) {
			// The mapping isn't there...we can't set it...
			this.log.warn("The ACL with ID[{}], specified as default for type [{}] is not available", aclId.getId(),
				type.getName());
			return;
		}

		final String aclDql = String.format("ALTER TYPE %s SET DEFAULT ACL %s IN %s", type.getName(),
			DfUtils.quoteString(acl.getObjectName()), DfUtils.quoteString(acl.getDomain()));
		DfUtils.closeQuietly(DfUtils.executeQuery(session, aclDql));
	}

	protected void setDefaultAspects(DctmImportContext ctx, IDfType type) throws DfException {
		CmfProperty<IDfValue> prop = this.cmfObject.getProperty(IntermediateProperty.DEFAULT_ASPECTS);
		if ((prop == null) || !prop.hasValues()) { return; }

		final IDfSession session = ctx.getSession();
		for (IDfValue v : prop) {
			IDfPersistentObject o = session.getObjectByQualification(
				String.format("dmc_aspect_type where object_name = %s", DfUtils.quoteString(v.asString())));
			if (o == null) {
				this.log.warn(
					"Type [{}] references non-existent aspect [{}] as a default aspect - can't replicate the setting because the aspect doesn't exist",
					type.getName(), v.asString());
				continue;
			}
			final String aclDql = String.format("ALTER TYPE %s ADD DEFAULT ASPECT %s", type.getName(), v.asString());
			// TODO: Should the aspect name be quoted? The docs don't say so...
			DfUtils.closeQuietly(DfUtils.executeQuery(session, aclDql));
		}
	}

	protected void setDefaultStorage(DctmImportContext ctx, IDfType type) throws DfException {
		CmfProperty<IDfValue> prop = this.cmfObject.getProperty(IntermediateProperty.DEFAULT_STORAGE);
		if ((prop == null) || !prop.hasValues()) { return; }

		final IDfSession session = ctx.getSession();

		final String storeName = prop.getValue().asString();
		IDfStore store = DfUtils.getStore(session, storeName);
		if (store == null) {
			this.log.warn(
				"Type [{}] references non-existent store [{}] as a default store - can't replicate the setting",
				type.getName(), storeName);
			return;
		}
		final String aclDql = String.format("ALTER TYPE %s SET DEFAULT STORAGE %s", type.getName(), store.getName());
		DfUtils.closeQuietly(DfUtils.executeQuery(session, aclDql));
	}

	@Override
	protected IDfType newObject(DctmImportContext ctx) throws DfException {
		String typeName = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue().asString();
		String superTypeName = this.cmfObject.getAttribute(DctmAttributes.SUPER_NAME).getValue().asString();
		final IDfSession session = ctx.getSession();
		IDfType superType = null;
		if (superTypeName.length() > 0) {
			superType = session.getType(superTypeName);
			if (superType == null) {
				// We require a supertype that doesn't exist
				throw new DfException(String.format(
					"Attempting to create type [%s] before its supertype [%s] is created", typeName, superTypeName));
			}
		}

		final int attrCount = this.cmfObject.getAttribute(DctmAttributes.ATTR_COUNT).getValue().asInteger();
		final int startPosition = this.cmfObject.getAttribute(DctmAttributes.START_POS).getValue().asInteger();
		final CmfAttribute<IDfValue> attrNames = this.cmfObject.getAttribute(DctmAttributes.ATTR_NAME);
		final CmfAttribute<IDfValue> attrTypes = this.cmfObject.getAttribute(DctmAttributes.ATTR_TYPE);
		final CmfAttribute<IDfValue> attrLengths = this.cmfObject.getAttribute(DctmAttributes.ATTR_LENGTH);
		final CmfAttribute<IDfValue> attrRepeating = this.cmfObject.getAttribute(DctmAttributes.ATTR_REPEATING);

		// Start the DQL
		final StringBuilder dql = new StringBuilder();
		dql.append("create type \"").append(typeName).append("\"");
		// Iterate through only the custom attributes of the type object and add them to the dql
		// string
		final boolean parens = (startPosition < attrCount);
		final String nl = String.format("%n");
		if (parens) {
			dql.append("(").append(nl);
		}
		for (int i = startPosition; i < attrCount; ++i) {
			// If we're not the first, we need a comma
			if (i > startPosition) {
				dql.append(",").append(nl);
			}
			dql.append("\t").append(attrNames.getValue(i).asString()).append("\t");
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
		if (parens) {
			dql.append(nl).append(")");
		}

		// Add the supertype phrase if needed
		dql.append(" with supertype ").append((superType != null) ? superTypeName : "null").append(" publish");

		if (this.log.isInfoEnabled()) {
			this.log.info(String.format("Creating new type [%s] with DQL:%n%n%s%n", typeName, dql));
		}

		IDfCollection resultCol = DfUtils.executeQuery(session, dql.toString(), IDfQuery.DF_QUERY);
		try {
			while (resultCol.next()) {
				IDfType type = castObject(session.getObject(resultCol.getId(DctmAttributes.NEW_OBJECT_ID)));
				setDefaultACL(ctx, type);
				setDefaultAspects(ctx, type);
				setDefaultStorage(ctx, type);
				// TODO: set default group?
				// TODO: set default lifecycle?
				// TODO: set check constraints?
				// TODO: set value assistance?
				// TODO: property defaults?
				// TODO: other type settings?
				return type;
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
		return Tools.equals(object.getName(), this.cmfObject.getAttribute(DctmAttributes.NAME).getValue().asString());
	}

	@Override
	protected void prepareForConstruction(IDfType object, boolean newObject, DctmImportContext context)
		throws DfException {
	}

	@Override
	protected boolean skipImport(DctmImportContext ctx) throws DfException, ImportException {
		IDfValue typeNameValue = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue();
		final String typeName = typeNameValue.asString();
		if (ctx.isSpecialType(typeName)) {
			this.log.warn(String.format("Will not import special type [%s]", typeName));
			return true;
		}
		// If the type name is the same as dmi_${objectId}, we skip it
		if (Tools.equals(typeName, String.format("dmi_%s", this.cmfObject.getId()))) { return false; }
		return super.skipImport(ctx);
	}

	@Override
	protected IDfType locateInCms(DctmImportContext ctx) throws DfException {
		return ctx.getSession().getType(this.cmfObject.getAttribute(DctmAttributes.NAME).getValue().asString());
	}

	@Override
	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext ctx) throws DfException {
		return null;
	}
}