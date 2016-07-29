package com.armedia.cmf.engine.alfresco.bulk.common;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.tools.LocalOrganizationStrategy;
import com.armedia.cmf.storage.CmfAttribute;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;
import com.armedia.commons.utilities.FileNameTools;

public class AlfrescoBulkOrganizationStrategy extends LocalOrganizationStrategy {

	public static final String NAME = "alfresco-bulk-import";

	public AlfrescoBulkOrganizationStrategy() {
		super(AlfrescoBulkOrganizationStrategy.NAME);
	}

	protected AlfrescoBulkOrganizationStrategy(String name) {
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
			return String.format("%s_%s", oldFrag, versionLabel);
		}
		return oldFrag;
	}

	@Override
	public String calculateBaseName(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		CmfProperty<?> paths = object.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if (paths == null) { return super.calculateBaseName(translator, object, info); }
		switch (object.getType()) {
			case DOCUMENT:
				return object.getBatchId();
			default:
				return object.getId();
		}
	}

	@Override
	protected List<String> calculatePath(CmfAttributeTranslator<?> translator, CmfObject<?> object,
		CmfContentInfo info) {
		CmfProperty<?> paths = object.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if (paths == null) { return super.calculatePath(translator, object, info); }

		List<String> ret = new ArrayList<String>();
		for (String p : FileNameTools.tokenize(paths.getValue().toString(), '/')) {
			ret.add(p);
		}
		return ret;
	}

	@Override
	protected String calculateAppendix(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		// TODO: How to identify which number to return?
		return "";
	}
}