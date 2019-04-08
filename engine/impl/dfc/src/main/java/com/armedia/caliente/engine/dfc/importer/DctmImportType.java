/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dfc.DctmObjectType;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValueMapper.Mapping;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.DfObjectNotFoundException;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfPersistentObject;
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

	private final List<Pair<AttributeProxy, AttributeProxy>> attributeMismatches = new ArrayList<>();
	private final Map<String, AttributeProxy> attributesMissing = new TreeMap<>();

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
		Mapping m = ctx.getValueMapper().getTargetMapping(CmfObject.Archetype.ACL, DctmAttributes.R_OBJECT_ID,
			aclId.getId());
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
			DfcUtils.quoteString(acl.getObjectName()), DfcUtils.quoteString(acl.getDomain()));
		DfcQuery.run(session, aclDql);
	}

	protected void setDefaultAspects(DctmImportContext ctx, IDfType type) throws DfException {
		CmfProperty<IDfValue> prop = this.cmfObject.getProperty(IntermediateProperty.DEFAULT_ASPECTS);
		if ((prop == null) || !prop.hasValues()) { return; }

		final IDfSession session = ctx.getSession();
		for (IDfValue v : prop) {
			IDfPersistentObject o = session.getObjectByQualification(
				String.format("dmc_aspect_type where object_name = %s", DfcUtils.quoteString(v.asString())));
			if (o == null) {
				this.log.warn(
					"Type [{}] references non-existent aspect [{}] as a default aspect - can't replicate the setting because the aspect doesn't exist",
					type.getName(), v.asString());
				continue;
			}
			final String aclDql = String.format("ALTER TYPE %s ADD DEFAULT ASPECT %s", type.getName(), v.asString());
			// TODO: Should the aspect name be quoted? The docs don't say so...
			DfcQuery.run(session, aclDql);
		}
	}

	protected void setDefaultStorage(DctmImportContext ctx, IDfType type) throws DfException {
		CmfProperty<IDfValue> prop = this.cmfObject.getProperty(IntermediateProperty.DEFAULT_STORAGE);
		if ((prop == null) || !prop.hasValues()) { return; }

		final IDfSession session = ctx.getSession();

		final String storeName = prop.getValue().asString();
		IDfStore store = DfcUtils.getStore(session, storeName);
		if (store == null) {
			this.log.warn(
				"Type [{}] references non-existent store [{}] as a default store - can't replicate the setting",
				type.getName(), storeName);
			return;
		}
		final String aclDql = String.format("ALTER TYPE %s SET DEFAULT STORAGE %s", type.getName(), store.getName());
		DfcQuery.run(session, aclDql);
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
		final CmfAttribute<IDfValue> attrQualified = this.cmfObject.getAttribute(DctmAttributes.ATTR_RESTRICTION);

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
			if (attrRepeating.getValue(i).asBoolean()) {
				dql.append(" repeating");
			}
			if (attrQualified.getValue(i).asInteger() != 0) {
				dql.append(" not qualified");
			}
		}
		if (parens) {
			dql.append(nl).append(")");
		}

		// Add the supertype phrase if needed
		dql.append(" with supertype ").append((superType != null) ? superTypeName : "null").append(" publish");

		if (this.log.isInfoEnabled()) {
			this.log.info("Creating new type [{}] with DQL:{}{}{}{}", typeName, Tools.NL, Tools.NL, dql, Tools.NL);
		}

		try (DfcQuery query = new DfcQuery(session, dql.toString(), DfcQuery.Type.DF_QUERY)) {
			while (query.hasNext()) {
				IDfType type = castObject(session.getObject(query.next().getId(DctmAttributes.NEW_OBJECT_ID)));
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
		}
	}

	@Override
	protected void finalizeConstruction(IDfType existingType, boolean newObject, DctmImportContext context)
		throws DfException, ImportException {
		super.finalizeConstruction(existingType, newObject, context);

		// If it's a new type being created, we don't need to do any of this
		if (newObject) { return; }

		boolean ok = true;
		final String typeName = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue().asString();
		final StringBuilder sb = new StringBuilder();
		final String nl = String.format("%n");
		if (!this.attributesMissing.isEmpty()) {
			final IDfSession session = context.getSession();
			final List<Pair<AttributeProxy, ? extends Exception>> errors = new ArrayList<>(
				this.attributesMissing.size());
			final String template = "ALTER TYPE %s ADD %s %s PUBLISH";
			for (AttributeProxy att : this.attributesMissing.values()) {
				DctmDataType dataType = DctmDataType.fromAttribute(att);
				final String dec = dataType.getDeclaration(att);
				try {
					DfcQuery.run(session, String.format(template, typeName, att.getName(), dec),
						DfcQuery.Type.DF_QUERY);
				} catch (Exception e) {
					ok = false;
					errors.add(Pair.of(att, e));
				}
			}

			if (!errors.isEmpty()) {
				ok = false;
				sb.append("\t").append("Source attributes not found on the target and couldn't be added:").append(nl);
				sb.append("\t").append(StringUtils.repeat('=', 60)).append(nl);
				for (Pair<AttributeProxy, ? extends Exception> err : errors) {
					sb.append("\t\t").append(err.getLeft()).append(": ").append(Tools.dumpStackTrace(err.getRight()))
						.append(")").append(nl);
				}
				sb.append(nl);
			}
		}

		if (!this.attributeMismatches.isEmpty()) {
			int errorCount = 0;
			final IDfSession session = context.getSession();
			final Map<String, List<String>> errors = new HashMap<>(this.attributeMismatches.size());
			final String template = "ALTER TYPE %s MODIFY (%s %s) PUBLISH";
			for (Pair<AttributeProxy, AttributeProxy> pair : this.attributeMismatches) {

				boolean thisOk = true;

				AttributeProxy srcAtt = pair.getLeft();
				AttributeProxy tgtAtt = pair.getRight();

				List<String> e = new ArrayList<>();
				errors.put(srcAtt.getName(), e);

				// If the difference is in repeatability, it's already a failure...
				if (srcAtt.isRepeating() != tgtAtt.isRepeating()) {
					e.add(String.format(
						"Source is %srepeating, but target is %srepeating - we can't fix this automatically",
						srcAtt.isRepeating() ? "" : "non-", tgtAtt.isRepeating() ? "" : "non-"));
					ok = false;
					thisOk = false;
					errorCount++;
				}

				// If the difference is in qualification, it's already a failure...
				if (srcAtt.isQualifiable() != tgtAtt.isQualifiable()) {
					e.add(String.format(
						"Source is %squalifiable, but target is %squalifiable - we can't fix this automatically",
						srcAtt.isQualifiable() ? "" : "non-", tgtAtt.isQualifiable() ? "" : "non-"));
					ok = false;
					thisOk = false;
					errorCount++;
				}

				// If the difference is in the base type, then it's already a failure...
				if (srcAtt.getDataType() != tgtAtt.getDataType()) {
					e.add("Attribute data types aren't compatible");
					ok = false;
					thisOk = false;
					errorCount++;
				}

				if (!thisOk) {
					continue;
				}

				DctmDataType dataType = DctmDataType.fromAttribute(srcAtt);
				final String dec = dataType.getDeclaration(srcAtt);
				try {
					DfcQuery.run(session, String.format(template, typeName, srcAtt.getName(), dec),
						DfcQuery.Type.DF_QUERY);
					context.trackWarning(this.cmfObject, "Modified DM_TYPE [%s] attribute [%s] to match [%s]", typeName,
						tgtAtt, srcAtt);
				} catch (Exception ex) {
					ok = false;
					errorCount++;
					e.add(Tools.dumpStackTrace(ex));
				}
			}

			if (errorCount > 0) {
				sb.append("\t").append("Target attributes that didn't match their source and couldn't be fixed:")
					.append(nl);
				sb.append("\t").append(StringUtils.repeat('=', 80)).append(nl);
				for (Pair<AttributeProxy, AttributeProxy> mismatch : this.attributeMismatches) {
					final AttributeProxy source = mismatch.getLeft();
					final AttributeProxy target = mismatch.getRight();
					final List<String> error = errors.get(source.getName());
					if ((error == null) || error.isEmpty()) {
						// No error, so no need to report
						continue;
					}

					sb.append("\t\tSOURCE: ").append(source).append(nl);
					sb.append("\t\tTARGET: ").append(target).append(nl);
					for (String s : error) {
						sb.append("\t\tERROR : ").append(s).append(nl);
					}
					sb.append(nl);
				}
			}
		}
		if (!ok) {
			throw new ImportException(String.format("Type declaration mismatch for [%s]:%n%n%s%n%n", typeName, sb));
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
	protected boolean isSameObject(IDfType existingType, DctmImportContext ctx) throws DfException, ImportException {
		if (!Tools.equals(existingType.getName(),
			this.cmfObject.getAttribute(DctmAttributes.NAME).getValue().asString())) {
			return false;
		}

		this.attributesMissing.clear();
		this.attributeMismatches.clear();

		CmfAttribute<IDfValue> nameAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_NAME);
		if (nameAtt == null) {
			throw new ImportException(
				String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_NAME));
		}
		CmfAttribute<IDfValue> repeatingAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_REPEATING);
		if (repeatingAtt == null) {
			throw new ImportException(
				String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_REPEATING));
		}
		CmfAttribute<IDfValue> typeAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_TYPE);
		if (typeAtt == null) {
			throw new ImportException(
				String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_TYPE));
		}
		CmfAttribute<IDfValue> lengthAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_LENGTH);
		if (lengthAtt == null) {
			throw new ImportException(
				String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_LENGTH));
		}
		CmfAttribute<IDfValue> restrictionAtt = this.cmfObject.getAttribute(DctmAttributes.ATTR_RESTRICTION);
		if (restrictionAtt == null) {
			throw new ImportException(
				String.format("Source type is missing the attribute %s", DctmAttributes.ATTR_RESTRICTION));
		}
		CmfAttribute<IDfValue> startPosAtt = this.cmfObject.getAttribute(DctmAttributes.START_POS);
		if (startPosAtt == null) {
			throw new ImportException(
				String.format("Source type is missing the attribute %s", DctmAttributes.START_POS));
		}

		// Now, make a cache of the source attributes in play
		final int startPosition = startPosAtt.getValue().asInteger();
		final int srcCount = nameAtt.getValueCount();
		for (int i = startPosition; i < srcCount; i++) {
			AttributeProxy att = new AttributeProxy(i, nameAtt, typeAtt, lengthAtt, repeatingAtt, restrictionAtt);
			this.attributesMissing.put(att.getName(), att);
		}

		// Here we explicitly ignore resolving the hierarchical conflict of an inherited
		// attribute that may or may not match what comes in the source. This is because
		// resolving that issue is a significant challenge all its own and we can't safely
		// do that without incurring a LOT of risk. So, we let the admins know of the mismatch
		// and let them take care of it in their own way...

		// Compare the source attribute declarations with the target attribute declarations...
		// We don't much care where the attribute was declared as long as it's available for use in
		// the current object type...
		final int tgtCount = existingType.getTypeAttrCount();
		final int tgtStart = existingType.getInt(DctmAttributes.START_POS);
		for (int i = tgtStart; i < tgtCount; i++) {
			final AttributeProxy tgtAttr = new AttributeProxy(existingType.getTypeAttr(i));
			final AttributeProxy srcAttr = this.attributesMissing.remove(tgtAttr.getName());
			if (srcAttr == null) {
				continue;
			}
			if (!srcAttr.isAssignableTo(tgtAttr)) {
				ctx.trackWarning(this.cmfObject, "DM_TYPE [%s] attribute [%s] did not match the source attribute [%s]",
					nameAtt.getValue().asString(), tgtAttr, srcAttr);
				this.attributeMismatches.add(Pair.of(srcAttr, tgtAttr));
			}
		}

		// We're considered identical if there are no missing attributes and
		// all matching attributes between source and target are "assignable".
		// At this point we can't check anything else...
		return this.attributesMissing.isEmpty() && this.attributeMismatches.isEmpty();
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
			this.log.warn("Will not import special type [{}]", typeName);
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
		final String typeName = this.cmfObject.getAttribute(DctmAttributes.NAME).getValue().asString();
		return ctx.getSession().getType(typeName);
	}

	@Override
	protected String generateSystemAttributesSQL(CmfObject<IDfValue> stored, IDfPersistentObject object,
		DctmImportContext ctx) throws DfException {
		return null;
	}
}