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
import com.armedia.cmf.storage.CmfValueCodec;
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
		CmfProperty<T> pathProp = object.getProperty(IntermediateProperty.JSAP_PARENT_TREE_IDS);
		if (pathProp == null) {
			pathProp = object.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		}
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
		CmfAttribute<T> latestVersionAtt = object
			.getAttribute(translator.decodeAttributeName(object.getType(), IntermediateAttribute.IS_LATEST_VERSION));
		final boolean isHeadVersion = ((latestVersionAtt != null) && latestVersionAtt.hasValues()
			&& translator.getCodec(latestVersionAtt.getType()).encodeValue(latestVersionAtt.getValue()).asBoolean());

		CmfProperty<T> vCounter = object.getProperty(IntermediateProperty.VERSION_COUNT);
		CmfProperty<T> vIndex = object.getProperty(IntermediateProperty.VERSION_INDEX);
		String appendix = null;
		final String versionPrefix = "0.";
		if (!primaryContent) {
			appendix = "";
		} else {
			if ((vCounter != null) && (vIndex != null) && vCounter.hasValues() && vIndex.hasValues()) {
				// Use the version index counter
				CmfValueCodec<T> vCounterCodec = translator.getCodec(vCounter.getType());
				CmfValueCodec<T> vIndexCodec = translator.getCodec(vIndex.getType());

				final int counter = vCounterCodec.encodeValue(vCounter.getValue()).asInteger();
				final int index = vIndexCodec.encodeValue(vIndex.getValue()).asInteger();
				if ((index < (counter - 1)) || !isHeadVersion) {
					final int width = String.format("%d", counter).length();

					String format = "v%s%d";
					if (width > 1) {
						format = String.format("v%%s%%0%dd", width);
					}
					appendix = String.format(format, versionPrefix, index);
				}
			} else {
				// Use the version string
				CmfAttribute<T> versionString = object.getAttribute(
					translator.decodeAttributeName(object.getType(), IntermediateAttribute.VERSION_LABEL));
				if ((versionString != null) && versionString.hasValues()) {
					final String v = translator.getCodec(versionString.getType()).encodeValue(versionString.getValue())
						.asString();
					appendix = String.format("v%s", v);
				}
			}
		}
		return newLocation(paths, baseName, null, null, appendix);
	}
}