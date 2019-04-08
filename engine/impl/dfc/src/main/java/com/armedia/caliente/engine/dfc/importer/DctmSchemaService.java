package com.armedia.caliente.engine.dfc.importer;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import com.armedia.caliente.engine.dfc.DctmAttributes;
import com.armedia.caliente.engine.dfc.DctmDataType;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.AttributeDeclaration;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.dynamic.transformer.mapper.schema.TypeDeclaration;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.tools.dfc.DfcQuery;
import com.armedia.caliente.tools.dfc.DfcUtils;
import com.documentum.fc.client.IDfLocalTransaction;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;

public class DctmSchemaService implements SchemaService {

	private static final Triple<String, String, String> TYPE_DQL = Triple.of("object",
		"select name from dm_type order by super_name, name", "name");
	private static final Triple<String, String, String> ASPECT_DQL = Triple.of("secondary",
		"select object_name from dmc_aspect_type order by object_name", "object_name");
	private static final String ASPECT_TABLE_DQL = "select object_name, i_attr_def from dmc_aspect_type where object_name = %s";
	private static final String ASPECT_ATTR_DQL = "select * from %s enable(return_top 1, optimize_top 1)";

	private static final boolean[] REPEATING = {
		false, true
	};

	private final IDfSession session;

	public DctmSchemaService(IDfSession session) {
		this.session = Objects.requireNonNull(session, "Must provide a non-null IDfSession instance");
	}

	protected Collection<String> getTypeNames(Triple<String, String, String> dql) throws SchemaServiceException {
		Set<String> names = new TreeSet<>();
		try {
			DfcQuery.run(this.session, dql.getMiddle(), DfcQuery.Type.DF_READ_QUERY,
				(c) -> names.add(c.getString(dql.getRight())));
		} catch (DfException e) {
			throw new SchemaServiceException(String.format("Failed to enumerate the existing %s types", dql.getLeft()),
				e);
		}
		return names;
	}

	@Override
	public Collection<String> getObjectTypeNames() throws SchemaServiceException {
		return getTypeNames(DctmSchemaService.TYPE_DQL);
	}

	@Override
	public TypeDeclaration getObjectTypeDeclaration(String typeName) throws SchemaServiceException {
		try {
			IDfType type = this.session.getType(typeName);
			if (type == null) { return null; }

			Map<String, AttributeDeclaration> attributes = new TreeMap<>();
			final int attributeCount = type.getTypeAttrCount();
			for (int i = type.getInt(DctmAttributes.START_POS); i < attributeCount; i++) {
				IDfAttr attr = type.getTypeAttr(i);
				String name = attr.getName();
				CmfValue.Type dataType = null;
				attributes.put(name, new AttributeDeclaration(name, dataType, false, attr.isRepeating()));
			}

			IDfPersistentObject typeInfo = this.session.getObjectByQualification(
				String.format("dmi_type_info where r_type_id = %s", DfcUtils.quoteString(type.getObjectId().getId())));
			Collection<String> secondaries = new TreeSet<>();
			final int secondaryCount = typeInfo.getValueCount(DctmAttributes.DEFAULT_ASPECTS);
			for (int i = 0; i < secondaryCount; i++) {
				secondaries.add(typeInfo.getRepeatingString(DctmAttributes.DEFAULT_ASPECTS, i));
			}
			return new TypeDeclaration(type.getName(), attributes.values(), secondaries, type.getSuperName());
		} catch (DfException e) {
			throw new SchemaServiceException(
				String.format("Failed to build the object type declaration for [%s]", typeName), e);
		}
	}

	@Override
	public Collection<String> getSecondaryTypeNames() throws SchemaServiceException {
		return getTypeNames(DctmSchemaService.ASPECT_DQL);
	}

	@Override
	public TypeDeclaration getSecondaryTypeDeclaration(String secondaryTypeName) throws SchemaServiceException {
		String dql = String.format(DctmSchemaService.ASPECT_TABLE_DQL, DfcUtils.quoteString(secondaryTypeName));
		try (DfcQuery query = new DfcQuery(this.session, dql, DfcQuery.Type.DF_READ_QUERY)) {
			if (!query.hasNext()) { return null; }
			final String attrDef = query.next().getString(DctmAttributes.I_ATTR_DEF);

			final Map<String, AttributeDeclaration> attributes = new TreeMap<>();
			if (!StringUtils.isBlank(attrDef)) {
				for (boolean repeating : DctmSchemaService.REPEATING) {
					final IDfLocalTransaction tx = DfcUtils.openTransaction(this.session);
					try {
						dql = String.format(DctmSchemaService.ASPECT_ATTR_DQL,
							String.format("%s_%s", attrDef, repeating ? "r" : "s"));
						try (DfcQuery query2 = new DfcQuery(this.session, dql, DfcQuery.Type.DF_READ_QUERY)) {
							if (!query.hasNext()) {
								continue;
							}
							// Now, introspect the column names/types
							IDfTypedObject c = query2.next();
							final int count = c.getAttrCount();
							for (int i = 0; i < count; i++) {
								IDfAttr attr = c.getAttr(i);

								// Skip the object ID...we're not interested
								if (StringUtils.equalsIgnoreCase(DctmAttributes.R_OBJECT_ID, attr.getName())) {
									continue;
								}

								// If this is a repeating attribute, ignore the position
								if (repeating
									&& StringUtils.equalsIgnoreCase(DctmAttributes.I_POSITION, attr.getName())) {
									continue;
								}

								DctmDataType type = DctmDataType.fromAttribute(attr);
								attributes.put(attr.getName(),
									new AttributeDeclaration(attr.getName(), type.getStoredType(), false, repeating));
							}
						} catch (final DfException e) {
							if (StringUtils.equalsIgnoreCase("DM_QUERY2_E_TABLE_NOT_FOUND", e.getMessageId())) {
								// No data for that aspect's attribtue attributes
								continue;
							}
							// Rethrow, so the outer catch clause processes it
							throw e;
						}
					} finally {
						DfcUtils.abortTransaction(this.session, tx);
					}
				}
			}

			// TODO: How to get the parent name? Is there such a thing in Documentum beyond Java?
			return new TypeDeclaration(secondaryTypeName, attributes.values(), null, null);
		} catch (DfException e) {
			throw new SchemaServiceException(
				String.format("Failed to build the secondary type declaration for [%s]", secondaryTypeName), e);
		}
	}

	@Override
	public void close() {
	}
}