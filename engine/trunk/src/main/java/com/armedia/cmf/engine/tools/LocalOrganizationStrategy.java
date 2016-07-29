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
	public String calculateDescriptor(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		final String attName = translator.decodeAttributeName(object.getType(), IntermediateAttribute.VERSION_LABEL);
		final CmfAttribute<?> versionLabelAtt = object.getAttribute(attName);
		String oldFrag = super.calculateDescriptor(translator, object, info);
		if ((versionLabelAtt != null) && versionLabelAtt.hasValues()) {
			final String versionLabel = versionLabelAtt.getValue().toString();
			if (StringUtils.isBlank(versionLabel)) { return oldFrag; }
			if (StringUtils.isBlank(oldFrag)) { return versionLabel; }
			return String.format("%s@%s", oldFrag, versionLabel);
		}
		return oldFrag;
	}

	@Override
	protected List<String> calculatePath(CmfAttributeTranslator<?> translator, CmfObject<?> object,
		CmfContentInfo info) {
		// Put it in the same path as it was in CMIS, but ensure each path component is
		// of a "universally-valid" format.
		CmfProperty<?> paths = object.getProperty(IntermediateProperty.PATH);

		List<String> ret = new ArrayList<String>();
		if (paths.hasValues()) {
			for (String p : FileNameTools.tokenize(paths.getValue().toString(), '/')) {
				ret.add(p);
			}
		}
		return ret;
	}

	@Override
	protected String calculateBaseName(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		CmfAttribute<?> name = object
			.getAttribute(translator.decodeAttributeName(object.getType(), IntermediateAttribute.NAME));
		return name.getValue().toString();
	}
}