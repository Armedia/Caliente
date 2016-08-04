package com.armedia.cmf.engine.alfresco.bulk.common;

import java.util.ArrayList;
import java.util.List;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.tools.LocalOrganizationStrategy;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
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
	protected <T> Location calculateLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		CmfProperty<T> pathProp = object.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if (pathProp == null) { return super.calculateLocation(translator, object, info); }

		final boolean primaryContent = (info.isDefaultRendition() && (info.getRenditionPage() == 0));

		List<String> paths = new ArrayList<String>();
		for (String p : FileNameTools.tokenize(pathProp.getValue().toString(), '/')) {
			paths.add(p);
		}

		if (!primaryContent) {
			// Ok...so this isn't the default rendition, so we have to add the object ID
			// at the end of the path
			paths.add(String.format("%s-renditions", object.getId()));
			paths.add(String.format("rendition-[%s]", info.getRenditionIdentifier()));
		}

		String baseName = object.getId();
		switch (object.getType()) {
			case DOCUMENT:
				baseName = object.getBatchId();
				if (!primaryContent) {
					baseName = String.format("page-%08x", info.getRenditionPage());
				}
			default:
				break;
		}
		return newLocation(paths, baseName, null, null, calculateAppendix(translator, object, info));
	}

	@Override
	protected <T> String calculateAppendix(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		CmfProperty<T> prop = object
			.getAttribute(translator.decodeAttributeName(object.getType(), IntermediateAttribute.VERSION_LABEL));
		if ((prop != null) && prop.hasValues()) {
			// TODO: Sequential numbers? How?
			final String versionString = translator.getCodec(prop.getType()).encodeValue(prop.getValue()).asString();
			return String.format("v%s", versionString);
		}
		return "";
	}
}