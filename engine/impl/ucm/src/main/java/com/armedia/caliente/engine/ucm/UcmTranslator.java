package com.armedia.caliente.engine.ucm;

import java.text.ParseException;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.ucm.model.UcmAtt;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public class UcmTranslator extends CmfAttributeTranslator<CmfValue> {

	private static final String UCM_PREFIX = "ucm:";
	private static final Map<CmfObject.Archetype, BidiMap<String, IntermediateAttribute>> ATTRIBUTE_MAPPINGS;

	static {
		Map<CmfObject.Archetype, BidiMap<String, IntermediateAttribute>> attributeMappings = new EnumMap<>(CmfObject.Archetype.class);

		BidiMap<String, IntermediateAttribute> am = null;

		am = new DualHashBidiMap<>();
		// BASE_TYPE_ID (DOCUMENT)
		// OBJECT_TYPE_ID (Document|...)
		am.put(UcmAtt.cmfUniqueURI.name(), IntermediateAttribute.OBJECT_ID);
		am.put(UcmAtt.cmfParentURI.name(), IntermediateAttribute.PARENT_ID);
		am.put(UcmAtt.cmfPath.name(), IntermediateAttribute.PATH);
		am.put(UcmAtt.dDocType.name(), IntermediateAttribute.OBJECT_TYPE_ID);
		am.put(UcmAtt.fFileName.name(), IntermediateAttribute.NAME);
		am.put(UcmAtt.fDisplayDescription.name(), IntermediateAttribute.DESCRIPTION);
		am.put(UcmAtt.dFormat.name(), IntermediateAttribute.CONTENT_STREAM_MIME_TYPE);
		am.put(UcmAtt.dFileSize.name(), IntermediateAttribute.CONTENT_STREAM_LENGTH);
		am.put(UcmAtt.dDocName.name(), IntermediateAttribute.VERSION_SERIES_ID);
		am.put(UcmAtt.cmfLatestVersion.name(), IntermediateAttribute.IS_LATEST_VERSION);
		am.put(UcmAtt.dRevisionID.name(), IntermediateAttribute.VERSION_LABEL);
		// am.put(UcmAtt.dRevisionID.name(), IntermediateAttribute.CHANGE_TOKEN);
		// TODO: These are the correct, infallible mappings for the creation/modification attributes
		// am.put(UcmAtt.dDocCreator.name(), IntermediateAttribute.CREATED_BY);
		// am.put(UcmAtt.dDocCreatedDate.name(), IntermediateAttribute.CREATION_DATE);
		// am.put(UcmAtt.dDocLastModifier.name(), IntermediateAttribute.LAST_MODIFIED_BY);
		// am.put(UcmAtt.dDocLastModifiedDate.name(), IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(UcmAtt.fCreator.name(), IntermediateAttribute.CREATED_BY);
		am.put(UcmAtt.fCreateDate.name(), IntermediateAttribute.CREATION_DATE);
		am.put(UcmAtt.fLastModifier.name(), IntermediateAttribute.LAST_MODIFIED_BY);
		am.put(UcmAtt.fLastModifiedDate.name(), IntermediateAttribute.LAST_MODIFICATION_DATE);
		am.put(UcmAtt.dCheckoutUser.name(), IntermediateAttribute.VERSION_SERIES_CHECKED_OUT_BY);
		am.put(UcmAtt.xComments.name(), IntermediateAttribute.CHECKIN_COMMENT);
		attributeMappings.put(CmfObject.Archetype.DOCUMENT, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		am = new DualHashBidiMap<>();
		// BASE_TYPE_ID (FOLDER)
		// OBJECT_TYPE_ID (Folder|...)
		am.put(UcmAtt.cmfUniqueURI.name(), IntermediateAttribute.OBJECT_ID);
		am.put(UcmAtt.cmfParentURI.name(), IntermediateAttribute.PARENT_ID);
		am.put(UcmAtt.cmfPath.name(), IntermediateAttribute.PATH);
		am.put(UcmAtt.fFolderName.name(), IntermediateAttribute.NAME);
		am.put(UcmAtt.fFolderDescription.name(), IntermediateAttribute.DESCRIPTION);
		am.put(UcmAtt.fCreator.name(), IntermediateAttribute.CREATED_BY);
		am.put(UcmAtt.fCreateDate.name(), IntermediateAttribute.CREATION_DATE);
		am.put(UcmAtt.fLastModifier.name(), IntermediateAttribute.LAST_MODIFIED_BY);
		am.put(UcmAtt.fLastModifiedDate.name(), IntermediateAttribute.LAST_MODIFICATION_DATE);
		attributeMappings.put(CmfObject.Archetype.FOLDER, UnmodifiableBidiMap.unmodifiableBidiMap(am));

		ATTRIBUTE_MAPPINGS = Tools.freezeMap(attributeMappings);
	}

	private static BidiMap<String, IntermediateAttribute> getAttributeMappings(CmfObject.Archetype type) {
		return UcmTranslator.ATTRIBUTE_MAPPINGS.get(type);
	}

	private static final CmfAttributeNameMapper MAPPER = new CmfAttributeNameMapper() {

		@Override
		public String encodeAttributeName(CmfObject.Archetype type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = UcmTranslator.getAttributeMappings(type);
			if (mappings != null) {
				// TODO: normalize the CMS attribute name
				IntermediateAttribute att = mappings.get(attributeName);
				if (att != null) { return att.encode(); }
			}
			return String.format("%s%s", UcmTranslator.UCM_PREFIX, attributeName);
		}

		@Override
		public String decodeAttributeName(CmfObject.Archetype type, String attributeName) {
			BidiMap<String, IntermediateAttribute> mappings = UcmTranslator.getAttributeMappings(type);
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
				UcmTranslator.UCM_PREFIX)) { return attributeName.substring(UcmTranslator.UCM_PREFIX.length()); }
			return attributeName;
		}
	};

	public UcmTranslator() {
		super(CmfValue.class, UcmTranslator.MAPPER);
	}

	@Override
	public CmfValueCodec<CmfValue> getCodec(CmfValue.Type type) {
		return CmfAttributeTranslator.getStoredValueCodec(type);
	}

	@Override
	public CmfValue getValue(CmfValue.Type type, Object value) throws ParseException {
		return new CmfValue(type, value);
	}
}