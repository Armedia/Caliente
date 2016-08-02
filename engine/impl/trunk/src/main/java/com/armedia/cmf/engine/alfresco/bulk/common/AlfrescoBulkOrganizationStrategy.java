package com.armedia.cmf.engine.alfresco.bulk.common;

import java.util.ArrayList;
import java.util.List;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.tools.LocalOrganizationStrategy;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.cmf.storage.CmfValueEncoderException;
import com.armedia.commons.utilities.FileNameTools;

public class AlfrescoBulkOrganizationStrategy extends LocalOrganizationStrategy {

	public static final String NAME = "alfrescoBulkImport";

	public AlfrescoBulkOrganizationStrategy() {
		super(AlfrescoBulkOrganizationStrategy.NAME);
	}

	protected AlfrescoBulkOrganizationStrategy(String name) {
		super(name);
	}

	@Override
	protected <T> List<String> calculatePath(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		CmfProperty<?> paths = object.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if (paths == null) { return super.calculatePath(translator, object, info); }

		List<String> ret = new ArrayList<String>();
		for (String p : FileNameTools.tokenize(paths.getValue().toString(), '/')) {
			ret.add(p);
		}

		if (!info.isDefaultRendition() || (info.getRenditionPage() > 0)) {
			// Ok...so this isn't the default rendition, so we have to add the object ID
			// at the end of the path
			ret.add(String.format("%s-renditions", object.getId()));
			ret.add(String.format("rendition-[%s]", info.getRenditionIdentifier()));
		}

		return ret;
	}

	@Override
	protected <T> String calculateBaseName(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		CmfProperty<?> paths = object.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if (paths == null) { return super.calculateBaseName(translator, object, info); }
		switch (object.getType()) {
			case DOCUMENT:
				String baseName = object.getBatchId();
				if (!info.isDefaultRendition() || (info.getRenditionPage() > 0)) {
					baseName = String.format("page-%08x", info.getRenditionPage());
				}
				return baseName;
			default:
				return object.getId();
		}
	}

	@Override
	protected <T> String calculateExtension(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		return null;
	}

	@Override
	protected <T> String calculateDescriptor(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		return null;
	}

	@Override
	protected <T> String calculateAppendix(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		CmfAttribute<T> att = object.getAttribute(IntermediateAttribute.IS_LATEST_VERSION);
		if (att != null) {
			try {
				CmfValue v = translator.getCodec(att.getType()).encodeValue(att.getValue());
				if (!v.asBoolean()) {
					// Not the latest version, so we have to append an appendix...which is
					// a version number or a sequential number
					v.hashCode();
				}
			} catch (CmfValueEncoderException e) {
				throw new RuntimeException(
					String.format("Failed to decode the value for the attribute [%s] for %s [%s](%s)", att.getName(),
						object.getType(), object.getLabel(), object.getId()),
					e);
			}
		}
		// TODO: How to identify which number to return?
		return "";
	}
}