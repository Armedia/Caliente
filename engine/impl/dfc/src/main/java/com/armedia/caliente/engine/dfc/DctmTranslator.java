package com.armedia.caliente.engine.dfc;

import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.dfc.importer.DctmImportContext;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.dfc.util.DfValueFactory;
import com.armedia.commons.utilities.Tools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

public final class DctmTranslator extends CmfAttributeTranslator<IDfValue> {
	private static final String DCTM_PREFIX = "dctm:";
	private static final Map<CmfType, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<CmfType, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<>(CmfType.class);

		BidiMap<String, IntermediateAttribute> am = null;

		am = new DualHashBidiMap<>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (USER)
		// OBJECT_TYPE_ID (DM_USER)
		am.put(DctmAttributes.USER_NAME, IntermediateAttribute.NAME);
		am.put(DctmAttributes.DESCRIPTION, IntermediateAttribute.DESCRIPTION);
		am.put(DctmAttributes.USER_GROUP_NAME, IntermediateAttribute.GROUP);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(DctmAttributes.DEFAULT_FOLDER, IntermediateAttribute.DEFAULT_FOLDER);
		am.put(DctmAttributes.USER_LOGIN_NAME, IntermediateAttribute.LOGIN_NAME);
		am.put(DctmAttributes.USER_LOGIN_DOMAIN, IntermediateAttribute.LOGIN_REALM);
		am.put(DctmAttributes.USER_OS_NAME, IntermediateAttribute.OS_NAME);
		am.put(DctmAttributes.USER_OS_DOMAIN, IntermediateAttribute.OS_REALM);
		am.put(DctmAttributes.USER_ADDRESS, IntermediateAttribute.EMAIL);
		am.put(DctmAttributes.USER_SOURCE, IntermediateAttribute.USER_SOURCE);
		attributeMappings.put(CmfType.USER, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (GROUP)
		// OBJECT_TYPE_ID (DM_GROUP)
		am.put(DctmAttributes.GROUP_NAME, IntermediateAttribute.NAME);
		am.put(DctmAttributes.GROUP_ADMIN, IntermediateAttribute.ADMINISTRATOR);
		am.put(DctmAttributes.GROUP_CLASS, IntermediateAttribute.GROUP_TYPE);
		am.put(DctmAttributes.DESCRIPTION, IntermediateAttribute.DESCRIPTION);
		am.put(DctmAttributes.OWNER_NAME, IntermediateAttribute.OWNER);
		am.put(DctmAttributes.GROUP_ADDRESS, IntermediateAttribute.EMAIL);
		am.put(DctmAttributes.GROUP_SOURCE, IntermediateAttribute.GROUP_SOURCE);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.LAST_MODIFICATION_DATE);
		attributeMappings.put(CmfType.GROUP, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (TYPE)
		// OBJECT_TYPE_ID (DM_TYPE)
		am.put(DctmAttributes.NAME, IntermediateAttribute.NAME);
		am.put(DctmAttributes.SUPER_NAME, IntermediateAttribute.SUPER_NAME);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(DctmAttributes.OWNER, IntermediateAttribute.OWNER);
		attributeMappings.put(CmfType.TYPE, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (FORMAT)
		// OBJECT_TYPE_ID (DM_FORMAT)
		am.put(DctmAttributes.NAME, IntermediateAttribute.NAME);
		am.put(DctmAttributes.DESCRIPTION, IntermediateAttribute.DESCRIPTION);
		attributeMappings.put(CmfType.FORMAT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		am.put(DctmAttributes.R_OBJECT_TYPE, IntermediateAttribute.OBJECT_TYPE_ID);
		// BASE_TYPE_ID (FOLDER)
		// OBJECT_TYPE_ID (DM_FOLDER|DM_CABINET|...)
		am.put(DctmAttributes.OBJECT_NAME, IntermediateAttribute.NAME);
		am.put(DctmAttributes.TITLE, IntermediateAttribute.DESCRIPTION);
		am.put(DctmAttributes.A_CONTENT_TYPE, IntermediateAttribute.CONTENT_STREAM_MIME_TYPE);
		am.put(DctmAttributes.OWNER_NAME, IntermediateAttribute.OWNER);
		am.put(DctmAttributes.OWNER_PERMIT, IntermediateAttribute.OWNER_PERMISSION);
		am.put(DctmAttributes.GROUP_NAME, IntermediateAttribute.GROUP);
		am.put(DctmAttributes.GROUP_PERMIT, IntermediateAttribute.GROUP_PERMISSION);
		am.put(DctmAttributes.R_CREATOR_NAME, IntermediateAttribute.CREATED_BY);
		am.put(DctmAttributes.R_CREATION_DATE, IntermediateAttribute.CREATION_DATE);
		am.put(DctmAttributes.R_ACCESS_DATE, IntermediateAttribute.LAST_ACCESS_DATE);
		am.put(DctmAttributes.R_MODIFIER, IntermediateAttribute.LAST_MODIFIED_BY);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(DctmAttributes.I_FOLDER_ID, IntermediateAttribute.PARENT_ID);
		am.put(DctmAttributes.R_FOLDER_PATH, IntermediateAttribute.PATH);
		am.put(DctmAttributes.ACL_NAME, IntermediateAttribute.ACL_NAME);
		am.put(DctmAttributes.ACL_DOMAIN, IntermediateAttribute.LOGIN_REALM);
		attributeMappings.put(CmfType.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(DctmAttributes.R_OBJECT_ID, IntermediateAttribute.OBJECT_ID);
		am.put(DctmAttributes.R_OBJECT_TYPE, IntermediateAttribute.OBJECT_TYPE_ID);
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (DM_DOCUMENT|...)
		am.put(DctmAttributes.OBJECT_NAME, IntermediateAttribute.NAME);
		am.put(DctmAttributes.TITLE, IntermediateAttribute.DESCRIPTION);
		am.put(DctmAttributes.A_CONTENT_TYPE, IntermediateAttribute.CONTENT_STREAM_MIME_TYPE);
		am.put(DctmAttributes.R_FULL_CONTENT_SIZE, IntermediateAttribute.CONTENT_STREAM_LENGTH);
		am.put(DctmAttributes.I_CHRONICLE_ID, IntermediateAttribute.VERSION_SERIES_ID);
		am.put(DctmAttributes.I_ANTECEDENT_ID, IntermediateAttribute.VERSION_ANTECEDENT_ID);
		am.put(DctmAttributes.R_VERSION_LABEL, IntermediateAttribute.VERSION_LABEL);
		am.put(DctmAttributes.OWNER_NAME, IntermediateAttribute.OWNER);
		am.put(DctmAttributes.OWNER_PERMIT, IntermediateAttribute.OWNER_PERMISSION);
		am.put(DctmAttributes.GROUP_NAME, IntermediateAttribute.GROUP);
		am.put(DctmAttributes.GROUP_PERMIT, IntermediateAttribute.GROUP_PERMISSION);
		am.put(DctmAttributes.R_CREATOR_NAME, IntermediateAttribute.CREATED_BY);
		am.put(DctmAttributes.R_CREATION_DATE, IntermediateAttribute.CREATION_DATE);
		am.put(DctmAttributes.R_ACCESS_DATE, IntermediateAttribute.LAST_ACCESS_DATE);
		am.put(DctmAttributes.R_MODIFIER, IntermediateAttribute.LAST_MODIFIED_BY);
		am.put(DctmAttributes.R_MODIFY_DATE, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(DctmAttributes.I_FOLDER_ID, IntermediateAttribute.PARENT_ID);
		am.put(DctmAttributes.ACL_NAME, IntermediateAttribute.ACL_NAME);
		am.put(DctmAttributes.ACL_DOMAIN, IntermediateAttribute.LOGIN_REALM);
		am.put(DctmAttributes.I_HAS_FOLDER, IntermediateAttribute.IS_LATEST_VERSION);
		am.put(DctmAttributes.R_LOCK_OWNER, IntermediateAttribute.VERSION_SERIES_CHECKED_OUT_BY);
		am.put(DctmAttributes.LOG_ENTRY, IntermediateAttribute.CHECKIN_COMMENT);
		am.put(DctmAttributes.I_VSTAMP, IntermediateAttribute.CHANGE_TOKEN);
		am.put(DctmAttributes.R_IMMUTABLE_FLAG, IntermediateAttribute.IS_IMMUTABLE);
		attributeMappings.put(CmfType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	private static final AttributeNameMapper MAPPER = new AttributeNameMapper() {
		@Override
		public String encodeAttributeName(CmfType type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = DctmTranslator.getAttributeMappings(type);
			if (mappings != null) {
				// TODO: normalize the CMS attribute name
				IntermediateAttribute att = mappings.get(attributeName);
				if (att != null) { return att.encode(); }
			}
			return String.format("%s%s", DctmTranslator.DCTM_PREFIX, attributeName.toLowerCase());
		}

		@Override
		public String decodeAttributeName(CmfType type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = DctmTranslator.getAttributeMappings(type);
			if (mappings != null) {
				String att = null;
				try {
					// TODO: normalize the intermediate attribute name
					att = mappings.getKey(IntermediateAttribute.decode(attributeName));
				} catch (IllegalArgumentException e) {
					att = null;
				}
				if (att != null) { return att; }
			}
			if (attributeName.startsWith(
				DctmTranslator.DCTM_PREFIX)) { return attributeName.substring(DctmTranslator.DCTM_PREFIX.length()); }
			return super.encodeAttributeName(type, attributeName);
		}
	};

	private static BidiMap<String, IntermediateAttribute> getAttributeMappings(CmfType type) {
		return DctmTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	private DctmTranslator() {
		// Nobody can instantiate
		super(IDfValue.class, DctmTranslator.MAPPER);
	}

	public static DctmDataType translateType(CmfDataType type) {
		switch (type) {
			case BOOLEAN:
				return DctmDataType.DF_BOOLEAN;
			case INTEGER:
				return DctmDataType.DF_INTEGER;
			case STRING:
				return DctmDataType.DF_STRING;
			case DOUBLE:
				return DctmDataType.DF_DOUBLE;
			case ID:
				return DctmDataType.DF_ID;
			case DATETIME:
				return DctmDataType.DF_TIME;
			default:
				return DctmDataType.DF_UNDEFINED;
		}
	}

	public static IDfType translateType(DctmImportContext ctx, CmfObject<IDfValue> object)
		throws ImportException, DfException {
		final IDfSession session = ctx.getSession();
		final String subType = object.getSubtype();
		IDfType type = session.getType(subType);

		if (type == null) {
			// TODO: Resort to a type map...
			DctmObjectType dctmType = DctmObjectType.decodeType(object.getType());
			if (dctmType == null) { return null; }
			type = session.getType(dctmType.getDmType());
		}

		if (type.isTypeOf("dm_cabinet") && ctx.isPathAltering()) {
			// If our context is putting things other than the root, then we need to demote this
			// into a folder...
			// TODO: What about custom cabinet types? What do those get demoted as? Do we have
			// to create a new folder subtype that adds the extra attributes?
			type = type.getSuperType();
		}
		return type;
	}

	@Override
	public CmfValueCodec<IDfValue> getCodec(CmfDataType type) {
		return DctmTranslator.translateType(type);
	}

	public static final CmfAttributeTranslator<IDfValue> INSTANCE = new DctmTranslator();

	@Override
	public IDfValue getValue(CmfDataType type, Object value) {
		return DfValueFactory.newValue(DctmTranslator.translateType(type).getDfConstant(), value);
	}

	@Override
	public String getDefaultSubtype(CmfType baseType) {
		DctmObjectType type = DctmObjectType.decodeType(baseType);
		if (type != null) { return type.getDmType(); }
		return null;
	}
}