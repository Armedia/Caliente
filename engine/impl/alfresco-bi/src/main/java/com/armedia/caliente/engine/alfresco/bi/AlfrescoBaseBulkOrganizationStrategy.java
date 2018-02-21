package com.armedia.caliente.engine.alfresco.bi;

import java.util.ArrayList;
import java.util.List;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.tools.LocalOrganizationStrategy;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeNameMapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

public abstract class AlfrescoBaseBulkOrganizationStrategy extends LocalOrganizationStrategy {

	public static final String BASE_DIR = "bulk-import-root";

	private final String versionPrefix;

	protected AlfrescoBaseBulkOrganizationStrategy(String name, String versionPrefix) {
		super(name);
		if (versionPrefix == null) {
			versionPrefix = "";
		}
		this.versionPrefix = versionPrefix;
	}

	@Override
	protected <T> Location calculateLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {

		int folderLevels = 3;
		// A maximum of 7 levels...
		folderLevels = Tools.ensureBetween(3, folderLevels, 7);

		final boolean primaryContent = (info.isDefaultRendition() && (info.getRenditionPage() == 0));

		List<String> paths = new ArrayList<>();
		// Make sure the contents all land in the bulk-import root location, so it's easy to point
		// the bulk importer at that directory and not import any unwanted crap
		paths.add(AlfrescoBaseBulkOrganizationStrategy.BASE_DIR);
		String fullObjectNumber = AlfCommon.addNumericPaths(paths, object.getNumber());

		CmfProperty<T> vdocProp = object.getProperty(IntermediateProperty.VDOC_HISTORY);
		final boolean vdoc;
		if (vdocProp != null) {
			CmfValueCodec<T> vdocCodec = translator.getCodec(vdocProp.getType());
			vdoc = (vdocProp.hasValues() && vdocCodec.encodeValue(vdocProp.getValue()).asBoolean());
		} else {
			vdoc = false;
		}

		String appendix = calculateVersionAppendix(translator, object, info, primaryContent, vdoc);

		if (vdoc) {
			paths.add(fullObjectNumber);
			paths.add(appendix);
			appendix = "";
		}

		String baseName = fullObjectNumber;
		if (!primaryContent) {
			// Ok...so this isn't the default rendition, so we have to add the object ID
			// at the end of the path
			paths.add(String.format("%s-renditions", baseName));
			paths.add(String.format("rendition-[%s]", info.getRenditionIdentifier()));
		}

		switch (object.getType()) {
			case DOCUMENT:
				if (primaryContent) {
					appendix = "";
				} else {
					baseName = AlfrescoBaseBulkOrganizationStrategy.generateRenditionName(object, info);
				}
			default:
				break;
		}

		return newLocation(paths, baseName, null, null, appendix);
	}

	protected <T> String calculateVersionAppendix(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info, boolean primaryContent, boolean vDoc) {
		CmfAttributeNameMapper nameMapper = translator.getAttributeNameMapper();
		boolean headVersion = false;
		switch (object.getType()) {
			case DOCUMENT:
				// The "latest version" attribute really only says which version is "CURRENT". In
				// systems like Documentum, the "current" version may not be the "latest" one...
				CmfProperty<T> p = object.getAttribute(
					nameMapper.decodeAttributeName(object.getType(), IntermediateAttribute.IS_LATEST_VERSION));
				headVersion = ((p != null) && p.hasValues()
					&& translator.getCodec(p.getType()).encodeValue(p.getValue()).asBoolean());
				break;
			case FOLDER:
				// Fall-through
			default:
				headVersion = true;
				break;
		}
		CmfProperty<T> vCounter = object.getProperty(IntermediateProperty.VERSION_COUNT);
		CmfProperty<T> vIndex = object.getProperty(IntermediateProperty.VERSION_INDEX);
		CmfProperty<T> vIndexHead = object.getProperty(IntermediateProperty.VERSION_HEAD_INDEX);
		String appendix = null;
		if (!primaryContent && !vDoc) {
			appendix = "";
		} else {
			if ((vCounter != null) && (vIndex != null) && (vIndexHead != null) && vCounter.hasValues()
				&& vIndex.hasValues() && vIndexHead.hasValues()) {
				// Use the version index counter
				CmfValueCodec<T> vCounterCodec = translator.getCodec(vCounter.getType());
				CmfValueCodec<T> vIndexCodec = translator.getCodec(vIndex.getType());
				CmfValueCodec<T> vIndexHeadCodec = translator.getCodec(vIndexHead.getType());

				final int counter = vCounterCodec.encodeValue(vCounter.getValue()).asInteger();
				final int index = vIndexCodec.encodeValue(vIndex.getValue()).asInteger();
				final int indexHead = vIndexHeadCodec.encodeValue(vIndexHead.getValue()).asInteger();

				final boolean lastIsHead = (counter == indexHead);

				if ((index > 0) && (vDoc || !lastIsHead || !headVersion)) {
					final int offset = (lastIsHead ? 1 : 0);
					final int width = String.format("%d", (counter - offset)).length();

					String format = "v%s%d";
					if (width > 1) {
						format = String.format("v%%s%%0%dd", width);
					}
					appendix = String.format(format, this.versionPrefix, index);
				}

				if (vDoc && headVersion) {
					appendix = String.format("%s,CURRENT", appendix);
				}
			} else {
				// Use the version string
				CmfAttribute<T> versionString = object.getAttribute(
					nameMapper.decodeAttributeName(object.getType(), IntermediateAttribute.VERSION_LABEL));
				if ((versionString != null) && versionString.hasValues()) {
					final String v = translator.getCodec(versionString.getType()).encodeValue(versionString.getValue())
						.asString();
					appendix = String.format("v%s", v);
				}
			}
		}
		return appendix;
	}

	public static String generateRenditionName(CmfObject<?> object, CmfContentStream info) {
		return String.format("%s-[%s]-%08x-%s", object.getId(), info.getRenditionIdentifier(), info.getRenditionPage(),
			Tools.coalesce(info.getModifier(), ""));
	}
}