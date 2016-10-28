package com.armedia.cmf.engine.tools;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfOrganizationStrategy;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.commons.utilities.FileNameTools;

public class LocalOrganizationStrategy extends CmfOrganizationStrategy {

	public static final String NAME = "localfs";

	public LocalOrganizationStrategy() {
		super(LocalOrganizationStrategy.NAME);
	}

	protected LocalOrganizationStrategy(String name) {
		super(name);
	}

	@Override
	protected <T> Location calculateLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		final List<String> containerSpec = calculateContainerSpec(translator, object, info);
		final String baseName = calculateBaseName(translator, object, info);
		final String descriptor = calculateDescriptor(translator, object, info);
		final String extension = calculateExtension(translator, object, info);
		final String appendix = calculateAppendix(translator, object, info);
		return newLocation(containerSpec, baseName, extension, descriptor, appendix);
	}

	protected <T> List<String> calculateContainerSpec(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		// Put it in the same path as it was in CMIS, but ensure each path component is
		// of a "universally-valid" format.
		CmfProperty<T> paths = object.getProperty(IntermediateProperty.PATH);

		List<String> ret = new ArrayList<String>();
		if (paths.hasValues()) {
			for (String p : FileNameTools.tokenize(paths.getValue().toString(), '/')) {
				ret.add(p);
			}
		}
		return ret;
	}

	protected <T> String calculateBaseName(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		CmfAttribute<?> name = object
			.getAttribute(translator.decodeAttributeName(object.getType(), IntermediateAttribute.NAME));
		return name.getValue().toString();
	}

	protected <T> String calculateExtension(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		return info.getExtension();
	}

	protected <T> String calculateDescriptor(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		final String attName = translator.decodeAttributeName(object.getType(), IntermediateAttribute.VERSION_LABEL);
		final CmfAttribute<?> versionLabelAtt = object.getAttribute(attName);
		String oldFrag = String.format("%s.%08x", info.getRenditionIdentifier(), info.getRenditionPage());
		if ((versionLabelAtt != null) && versionLabelAtt.hasValues()) {
			final String versionLabel = versionLabelAtt.getValue().toString();
			if (StringUtils.isBlank(versionLabel)) { return oldFrag; }
			if (StringUtils.isBlank(oldFrag)) { return versionLabel; }
			return String.format("%s@%s", oldFrag, versionLabel);
		}
		return oldFrag;
	}

	protected <T> String calculateAppendix(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentInfo info) {
		return "";
	}
}