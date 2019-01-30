/**
 *
 */

package com.armedia.caliente.engine.sharepoint;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

/**
 * @author diego
 *
 */
public final class ShptTranslator extends CmfAttributeTranslator<CmfValue> {

	private static final String SHPT_PREFIX = "shpt:";
	private static final Map<CmfObject.Archetype, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<CmfObject.Archetype, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<>(CmfObject.Archetype.class);

		BidiMap<String, IntermediateAttribute> am = null;

		am = new DualHashBidiMap<>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (USER)
		// OBJECT_TYPE_ID (DM_USER)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.LOGIN_NAME.name, IntermediateAttribute.LOGIN_NAME);
		am.put(ShptAttributes.LOGIN_DOMAIN.name, IntermediateAttribute.LOGIN_REALM);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		attributeMappings.put(CmfObject.Archetype.USER, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (GROUP)
		// OBJECT_TYPE_ID (DM_GROUP)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.GROUP_OWNER.name, IntermediateAttribute.OWNER);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		attributeMappings.put(CmfObject.Archetype.GROUP, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (TYPE)
		// OBJECT_TYPE_ID (DM_TYPE)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		attributeMappings.put(CmfObject.Archetype.TYPE, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (FORMAT)
		// OBJECT_TYPE_ID (DM_FORMAT)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.DESCRIPTION.name, IntermediateAttribute.DESCRIPTION);
		attributeMappings.put(CmfObject.Archetype.FORMAT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (FOLDER)
		// OBJECT_TYPE_ID (DM_FOLDER|DM_CABINET|...)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.TITLE.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.CONTENT_TYPE.name, IntermediateAttribute.CONTENT_STREAM_MIME_TYPE);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		am.put(ShptAttributes.GROUP.name, IntermediateAttribute.GROUP);
		am.put(ShptAttributes.CREATOR.name, IntermediateAttribute.CREATED_BY);
		am.put(ShptAttributes.CREATE_DATE.name, IntermediateAttribute.CREATION_DATE);
		am.put(ShptAttributes.ACCESS_DATE.name, IntermediateAttribute.LAST_ACCESS_DATE);
		am.put(ShptAttributes.MODIFIER.name, IntermediateAttribute.LAST_MODIFIED_BY);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(ShptAttributes.PARENTS.name, IntermediateAttribute.PARENT_ID);
		am.put(ShptAttributes.PATHS.name, IntermediateAttribute.PATH);
		attributeMappings.put(CmfObject.Archetype.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		am.put(ShptAttributes.OBJECT_ID.name, IntermediateAttribute.OBJECT_ID);
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (DM_DOCUMENT|...)
		am.put(ShptAttributes.OBJECT_NAME.name, IntermediateAttribute.NAME);
		am.put(ShptAttributes.TITLE.name, IntermediateAttribute.DESCRIPTION);
		am.put(ShptAttributes.CONTENT_TYPE.name, IntermediateAttribute.CONTENT_STREAM_MIME_TYPE);
		am.put(ShptAttributes.CONTENT_SIZE.name, IntermediateAttribute.CONTENT_STREAM_LENGTH);
		am.put(ShptAttributes.OWNER.name, IntermediateAttribute.OWNER);
		am.put(ShptAttributes.CREATOR.name, IntermediateAttribute.CREATED_BY);
		am.put(ShptAttributes.CREATE_DATE.name, IntermediateAttribute.CREATION_DATE);
		am.put(ShptAttributes.MODIFIER.name, IntermediateAttribute.LAST_MODIFIED_BY);
		am.put(ShptAttributes.MODIFICATION_DATE.name, IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(ShptAttributes.PARENTS.name, IntermediateAttribute.PARENT_ID);
		am.put(ShptAttributes.VERSION.name, IntermediateAttribute.VERSION_LABEL);
		am.put(ShptAttributes.VERSION_TREE.name, IntermediateAttribute.VERSION_SERIES_ID);
		am.put(ShptAttributes.VERSION_PRIOR.name, IntermediateAttribute.VERSION_ANTECEDENT_ID);
		attributeMappings.put(CmfObject.Archetype.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	public static ShptTranslator INSTANCE = new ShptTranslator();

	private static BidiMap<String, IntermediateAttribute> getAttributeMappings(CmfObject.Archetype type) {
		return ShptTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	private static final CmfAttributeNameMapper MAPPER = new CmfAttributeNameMapper() {

		@Override
		public String encodeAttributeName(CmfObject.Archetype type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = ShptTranslator.getAttributeMappings(type);
			if (mappings != null) {
				// TODO: normalize the CMS attribute name
				IntermediateAttribute att = mappings.get(attributeName);
				if (att != null) { return att.encode(); }
			}
			return String.format("%s%s", ShptTranslator.SHPT_PREFIX, attributeName);
		}

		@Override
		public String decodeAttributeName(CmfObject.Archetype type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = ShptTranslator.getAttributeMappings(type);
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
				ShptTranslator.SHPT_PREFIX)) { return attributeName.substring(ShptTranslator.SHPT_PREFIX.length()); }
			return super.decodeAttributeName(type, attributeName);
		}

	};

	public ShptTranslator() {
		super(CmfValue.class, ShptTranslator.MAPPER);
	}

	@Override
	public CmfValueCodec<CmfValue> getCodec(CmfValue.Type type) {
		return CmfAttributeTranslator.getStoredValueCodec(type);
	}

	@Override
	public CmfValue getValue(CmfValue.Type type, Object value) throws ParseException {
		try {
			return new CmfValue(type, value);
		} catch (ParseException e) {
			throw new RuntimeException("Exception raised while creating a new value", e);
		}
	}
}