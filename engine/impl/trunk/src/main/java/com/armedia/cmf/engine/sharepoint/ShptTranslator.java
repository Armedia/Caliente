/**
 *
 */

package com.armedia.cmf.engine.sharepoint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.documentum.common.DctmDocument;
import com.armedia.cmf.engine.documentum.common.DctmFolder;
import com.armedia.cmf.engine.documentum.common.DctmSysObject;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.cmf.storage.StoredValueEncoderException;
import com.armedia.cmf.storage.UnsupportedObjectTypeException;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public final class ShptTranslator extends ObjectStorageTranslator<ShptObject<?>, StoredValue> {

	private static final Map<StoredObjectType, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;
	private static final Map<StoredObjectType, BidiMap<String, IntermediateProperty>> PROPERTY_MAPPINGS;

	static {
		Map<StoredObjectType, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<StoredObjectType, BidiMap<String, IntermediateAttribute>>(
			StoredObjectType.class);
		Map<StoredObjectType, BidiMap<String, IntermediateProperty>> propertyMappings = new EnumMap<StoredObjectType, BidiMap<String, IntermediateProperty>>(
			StoredObjectType.class);

		BidiMap<String, IntermediateAttribute> am = null;
		BidiMap<String, IntermediateProperty> pm = null;

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (USER)
		// OBJECT_TYPE (DM_USER)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.OBJECT_NAME);
		am.put(ShptAttributes.DOMAIN.name, IntermediateAttribute.AUTH_REALM);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.MODIFICATION_DATE);
		attributeMappings.put(StoredObjectType.USER, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.USER, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (GROUP)
		// OBJECT_TYPE (DM_GROUP)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.OBJECT_NAME);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.GROUP_OWNER.name, IntermediateAttribute.OWNER);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.MODIFICATION_DATE);
		attributeMappings.put(StoredObjectType.GROUP, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.GROUP, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (ACL)
		// OBJECT_TYPE (DM_ACL)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.OBJECT_NAME);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		attributeMappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.ACL, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (TYPE)
		// OBJECT_TYPE (DM_TYPE)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.OBJECT_NAME);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.MODIFICATION_DATE);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		attributeMappings.put(StoredObjectType.TYPE, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.TYPE, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (FORMAT)
		// OBJECT_TYPE (DM_FORMAT)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.OBJECT_NAME);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		attributeMappings.put(StoredObjectType.FORMAT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.FORMAT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (FOLDER)
		// OBJECT_TYPE (DM_FOLDER|DM_CABINET|...)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.OBJECT_NAME);
		am.put(ShptAttributes.TITLE.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.CONTENT_TYPE.name, IntermediateAttribute.CONTENT_TYPE);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		am.put(ShptAttributes.GROUP.name, IntermediateAttribute.GROUP);
		am.put(ShptAttributes.CREATOR.name, IntermediateAttribute.CREATOR);
		am.put(ShptAttributes.CREATE_DATE.name, IntermediateAttribute.CREATE_DATE);
		am.put(ShptAttributes.ACCESS_DATE.name, IntermediateAttribute.ACCESS_DATE);
		am.put(ShptAttributes.MODIFIER.name, IntermediateAttribute.MODIFIER);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.MODIFICATION_DATE);
		am.put(ShptAttributes.PARENTS.name, IntermediateAttribute.PARENTS);
		am.put(ShptAttributes.PATHS.name, IntermediateAttribute.PATHS);
		attributeMappings.put(StoredObjectType.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		pm.put(DctmSysObject.TARGET_PATHS, IntermediateProperty.TARGET_PATHS);
		pm.put(DctmSysObject.TARGET_PARENTS, IntermediateProperty.TARGET_PARENTS);
		pm.put(DctmFolder.USERS_WITH_DEFAULT_FOLDER, IntermediateProperty.USERS_WITH_DEFAULT_FOLDER);
		pm.put(DctmFolder.USERS_DEFAULT_FOLDER_PATHS, IntermediateProperty.USERS_DEFAULT_FOLDER_PATHS);
		propertyMappings.put(StoredObjectType.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (FOLDER)
		// OBJECT_TYPE (DM_DOCUMENT|...)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.OBJECT_NAME);
		am.put(ShptAttributes.TITLE.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.CONTENT_TYPE.name, IntermediateAttribute.CONTENT_TYPE);
		am.put(ShptAttributes.CONTENT_SIZE.name, IntermediateAttribute.CONTENT_SIZE);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		am.put(ShptAttributes.CREATOR.name, IntermediateAttribute.CREATOR);
		am.put(ShptAttributes.CREATE_DATE.name, IntermediateAttribute.CREATE_DATE);
		am.put(ShptAttributes.MODIFIER.name, IntermediateAttribute.MODIFIER);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.MODIFICATION_DATE);
		am.put(ShptAttributes.PARENTS.name, IntermediateAttribute.PARENTS);

		attributeMappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		pm.put(DctmSysObject.TARGET_PATHS, IntermediateProperty.TARGET_PATHS);
		pm.put(DctmSysObject.TARGET_PARENTS, IntermediateProperty.TARGET_PARENTS);
		pm.put(DctmDocument.CONTENTS, IntermediateProperty.CONTENTS);
		pm.put(DctmSysObject.VERSION_PATCHES, IntermediateProperty.VERSION_PATCHES);
		pm.put(DctmSysObject.PATCH_ANTECEDENT, IntermediateProperty.PATCH_ANTECEDENT);
		propertyMappings.put(StoredObjectType.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		am = new DualHashBidiMap<String, IntermediateAttribute>();
		pm = new DualHashBidiMap<String, IntermediateProperty>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// OBJECT_CLASS (CONTENT)
		// OBJECT_TYPE (DMR_CONTENT|...)
		am.put(ShptAttributes.CONTENT_TYPE.name, IntermediateAttribute.CONTENT_TYPE);
		am.put(ShptAttributes.CONTENT_SIZE.name, IntermediateAttribute.CONTENT_SIZE);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.MODIFICATION_DATE);
		attributeMappings.put(StoredObjectType.CONTENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));
		propertyMappings.put(StoredObjectType.CONTENT, UnmodifiableBidiMap.unmodifiableBidiMap(pm));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
		PROPERTY_MAPPINGS = Tools.freezeMap(propertyMappings);
	}

	private static class Codec implements StoredValueCodec<StoredValue> {

		private final StoredDataType type;

		private Codec(StoredDataType type) {
			this.type = type;
		}

		@Override
		public String encodeValue(StoredValue value) throws StoredValueEncoderException {
			if (value == null) { return null; }
			return value.asString();
		}

		@Override
		public StoredValue decodeValue(String value) throws StoredValueDecoderException {
			try {
				return new StoredValue(this.type, value);
			} catch (ParseException e) {
				throw new StoredValueDecoderException(String.format("Failed to decode the %s value contained in [%s]",
					this.type, value), e);
			}
		}

	}

	private static final StoredValueCodec<StoredValue> BOOLEAN = new Codec(StoredDataType.BOOLEAN);
	private static final StoredValueCodec<StoredValue> INTEGER = new Codec(StoredDataType.INTEGER);
	private static final StoredValueCodec<StoredValue> DOUBLE = new Codec(StoredDataType.DOUBLE);
	private static final StoredValueCodec<StoredValue> STRING = new Codec(StoredDataType.STRING);
	private static final StoredValueCodec<StoredValue> ID = new Codec(StoredDataType.ID);
	private static final StoredValueCodec<StoredValue> TIME = new Codec(StoredDataType.TIME) {
		private final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";

		private Date parse(String value) throws ParseException {
			return new SimpleDateFormat(this.DATE_FORMAT).parse(value);
		}

		@Override
		public String encodeValue(StoredValue value) throws StoredValueEncoderException {
			if (value == null) { return null; }
			try {
				return DateFormatUtils.format(value.asTime(), this.DATE_FORMAT);
			} catch (ParseException e) {
				throw new StoredValueEncoderException(String.format("Failed to encode the date contained in [%s]",
					value), e);
			}
		}

		@Override
		public StoredValue decodeValue(String value) throws StoredValueDecoderException {
			if (value == null) { return null; }
			try {
				return new StoredValue(parse(value));
			} catch (ParseException e) {
				throw new StoredValueDecoderException(
					String.format("Failed to parse the date contained in [%s]", value), e);
			}
		}
	};

	private static final Map<StoredDataType, StoredValueCodec<StoredValue>> CODECS;

	static {
		Map<StoredDataType, StoredValueCodec<StoredValue>> codecs = new EnumMap<StoredDataType, StoredValueCodec<StoredValue>>(
			StoredDataType.class);
		codecs.put(StoredDataType.BOOLEAN, ShptTranslator.BOOLEAN);
		codecs.put(StoredDataType.INTEGER, ShptTranslator.INTEGER);
		codecs.put(StoredDataType.DOUBLE, ShptTranslator.DOUBLE);
		codecs.put(StoredDataType.STRING, ShptTranslator.STRING);
		codecs.put(StoredDataType.ID, ShptTranslator.ID);
		codecs.put(StoredDataType.TIME, ShptTranslator.TIME);
		CODECS = Collections.unmodifiableMap(codecs);
	}

	public static ShptTranslator INSTANCE = new ShptTranslator();

	private ShptTranslator() {
		// Avoid instantiation
	}

	@Override
	protected StoredObjectType doDecodeObjectType(ShptObject<?> object) throws UnsupportedObjectTypeException {
		return object.getStoredType();
	}

	@Override
	protected Class<? extends ShptObject<?>> doDecodeObjectType(StoredObjectType type)
		throws UnsupportedObjectTypeException {
		switch (type) {
			case DOCUMENT:
				return ShptFile.class;
			case FOLDER:
				return ShptFolder.class;
			case USER:
				return ShptUser.class;
			case GROUP:
				return ShptGroup.class;
			default:
				return null;
		}
	}

	@Override
	protected String doGetObjectId(ShptObject<?> object) throws Exception {
		return object.getId();
	}

	@Override
	public StoredValueCodec<StoredValue> getCodec(StoredDataType type) {
		return ShptTranslator.CODECS.get(type);
	}

	private BidiMap<String, IntermediateAttribute> getAttributeMappings(StoredObjectType type) {
		return ShptTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	private BidiMap<String, IntermediateProperty> getPropertyMappings(StoredObjectType type) {
		return ShptTranslator.PROPERTY_MAPPINGS.get(type);
	}

	@Override
	public String encodeAttributeName(StoredObjectType type, String attributeName) {
		BidiMap<String, IntermediateAttribute> mappings = getAttributeMappings(type);
		if (mappings != null) {
			// TODO: normalize the CMS attribute name
			IntermediateAttribute att = mappings.get(attributeName);
			if (att != null) { return att.encode(); }
		}
		return super.encodeAttributeName(type, attributeName);
	}

	@Override
	public String decodeAttributeName(StoredObjectType type, String attributeName) {
		BidiMap<String, IntermediateAttribute> mappings = getAttributeMappings(type);
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
		return super.decodeAttributeName(type, attributeName);
	}

	@Override
	public String encodePropertyName(StoredObjectType type, String propertyName) {
		BidiMap<String, IntermediateProperty> mappings = getPropertyMappings(type);
		if (mappings != null) {
			// TODO: normalize the CMS property name
			IntermediateProperty prop = mappings.get(propertyName);
			if (prop != null) { return prop.encode(); }
		}
		return super.encodePropertyName(type, propertyName);
	}

	@Override
	public String decodePropertyName(StoredObjectType type, String propertyName) {
		BidiMap<String, IntermediateProperty> mappings = getPropertyMappings(type);
		if (mappings != null) {
			String prop = null;
			try {
				// TODO: normalize the intermediate property name
				prop = mappings.getKey(IntermediateProperty.decode(propertyName));
			} catch (IllegalArgumentException e) {
				prop = null;
			}
			if (prop != null) { return prop; }
		}
		return super.decodePropertyName(type, propertyName);
	}
}