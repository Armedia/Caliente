/**
 *
 */

package com.armedia.caliente.engine.documentum.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;

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

	private static class AttributeProxy implements IDfAttr {
		private final String id;
		private final String name;
		private final int type;
		private final int length;
		private final boolean repeating;
		private final boolean qualifiable;

		private AttributeProxy(IDfAttr attr) {
			this.id = attr.getId();
			this.name = attr.getName();
			this.type = attr.getDataType();
			this.length = attr.getLength();
			this.repeating = attr.isRepeating();
			this.qualifiable = attr.isQualifiable();
		}

		private AttributeProxy(int pos, CmfAttribute<IDfValue> name, CmfAttribute<IDfValue> type,
			CmfAttribute<IDfValue> length, CmfAttribute<IDfValue> repeating, CmfAttribute<IDfValue> restriction) {
			this.id = null;
			this.name = name.getValue(pos).asString();
			this.type = type.getValue(pos).asInteger();
			this.length = length.getValue(pos).asInteger();
			this.repeating = repeating.getValue(pos).asBoolean();
			this.qualifiable = !restriction.getValue(pos).asBoolean();
		}

		@Override
		public int getAllowedLength(String value) {
			return this.length;
		}

		@Override
		public int getDataType() {
			return this.type;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public int getLength() {
			return this.length;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean isQualifiable() {
			return this.qualifiable;
		}

		@Override
		public boolean isRepeating() {
			return this.repeating;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) { return true; }
			if (!IDfAttr.class.isInstance(obj)) { return false; }
			final IDfAttr other = IDfAttr.class.cast(obj);
			// if (!Tools.equals(this.id, other.getId())) { return false; }
			if (!Tools.equals(this.name, other.getName())) { return false; }
			if (this.type != other.getDataType()) { return false; }
			if (this.length != other.getLength()) { return false; }
			if (this.repeating != other.isRepeating()) { return false; }
			if (this.qualifiable != other.isQualifiable()) { return false; }
			return true;
		}

		public boolean isAssignableTo(IDfAttr other) {
			// if (!Tools.equals(this.id, other.getId())) { return false; }
			if (!Tools.equals(this.name, other.getName())) { return false; }
			if (this.type != other.getDataType()) { return false; }
			if ((this.type == IDfType.DF_STRING) && (this.length > other.getLength())) { return false; }
			if (this.repeating != other.isRepeating()) { return false; }
			if (this.qualifiable != other.isQualifiable()) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return String.format("IDfAttr [name=%s, type=%s, length=%s, repeating=%s, qualifiable=%s]", this.name,
				this.type, this.length, this.repeating, this.qualifiable);
		}
	}

	@Override
	protected IDfType locateInCms(DctmImportContext ctx) throws ImportException, DfException {
		// In order for a type to match an existing one, the existing type must support ALL the
		// incoming attributes from the source type (though not necessarily in the same order), and
		// all of them must be of the same type (and length, if applicable). Since we can't easily
		// check allowed values and valueassist (yet), that's all we look for. This is the
		// return logic:
		// * If there is no same-named type, return null
		// * If we have a compatible match, we return the existing type.
		// ( a compatible match is one where all source attributes can be directly copied onto
		// target attributes without any type conversion )
		// * If we don't have a compatible match, this is an error and we raise an exception
		final String typeName = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue().asString();
		final IDfType existingType = ctx.getSession().getType(typeName);
		if (existingType == null) { return null; }

		CmfAttribute<IDfValue> nameAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_NAME);
		if (nameAtt == null) { throw new ImportException(
			String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_NAME)); }
		CmfAttribute<IDfValue> repeatingAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_REPEATING);
		if (repeatingAtt == null) { throw new ImportException(
			String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_REPEATING)); }
		CmfAttribute<IDfValue> typeAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_TYPE);
		if (typeAtt == null) { throw new ImportException(
			String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_TYPE)); }
		CmfAttribute<IDfValue> lengthAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_LENGTH);
		if (lengthAtt == null) { throw new ImportException(
			String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_LENGTH)); }
		CmfAttribute<IDfValue> restrictionAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_RESTRICTION);
		if (restrictionAtt == null) { throw new ImportException(
			String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_RESTRICTION)); }

		// Now, make a cache of the source attributes in play
		Map<String, AttributeProxy> srcAttributes = new TreeMap<>();
		final int srcCount = nameAtt.getValueCount();
		for (int i = 0; i < srcCount; i++) {
			AttributeProxy att = new AttributeProxy(i, nameAtt, typeAtt, lengthAtt, repeatingAtt, restrictionAtt);
			srcAttributes.put(att.getName(), att);
		}

		// Compare the source attribute declarations with the target attribute declarations...
		// We don't much care where the attribute was declared as long as it's available for use in
		// the current object type...
		List<Pair<? extends IDfAttr, ? extends IDfAttr>> mismatches = new ArrayList<>();
		final int tgtCount = existingType.getTypeAttrCount();
		for (int i = 0; i < tgtCount; i++) {
			final AttributeProxy tgtAttr = new AttributeProxy(existingType.getTypeAttr(i));
			final AttributeProxy srcAttr = srcAttributes.remove(tgtAttr.getName());
			if (srcAttr == null) {
				continue;
			}
			if (!srcAttr.isAssignableTo(tgtAttr)) {
				mismatches.add(Pair.of(srcAttr, tgtAttr));
			}
		}

		// No errors...we have a compatible match!
		if (mismatches.isEmpty() && srcAttributes.isEmpty()) { return existingType; }

		// The match is incompatible... we don't take the shortcut of comparing counts because
		// we want to be able to identify mismatches completely, for easy identification and
		// potential remediation by the operator...
		StringBuilder sb = new StringBuilder();
		final String nl = String.format("%n");
		if (!srcAttributes.isEmpty()) {
			sb.append("\t").append("Source attributes not found on the target:").append(nl);
			sb.append("\t").append("==================================================").append(nl);
			for (IDfAttr att : srcAttributes.values()) {
				sb.append("\t\t").append(att.toString()).append(nl);
			}
			sb.append(nl);
		}
		if (!mismatches.isEmpty()) {
			sb.append("\t").append("Target attributes that don't match their source:").append(nl);
			sb.append("\t").append("==================================================").append(nl);
			for (Pair<? extends IDfAttr, ? extends IDfAttr> mismatch : mismatches) {
				sb.append("\t\tSOURCE: ").append(mismatch.getLeft()).append(nl);
				sb.append("\t\tTARGET: ").append(mismatch.getRight()).append(nl);
				sb.append(nl);
			}
		}
		throw new ImportException(String.format("Type declaration mismatch for [%s]:%n%n%s%n%n", typeName, sb));
	}

	@Override
	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext ctx) throws DfException {
		return null;
	}
}