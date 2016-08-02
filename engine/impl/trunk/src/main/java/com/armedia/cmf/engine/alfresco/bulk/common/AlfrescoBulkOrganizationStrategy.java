package com.armedia.cmf.engine.alfresco.bulk.common;

import java.util.ArrayList;
import java.util.List;

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
	public String calculateDescriptor(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		// No descriptor...
		return null;
	}

	@Override
	protected String calculateAppendix(CmfAttributeTranslator<?> translator, CmfObject<?> object, CmfContentInfo info) {
		// TODO: How to identify which number to return?
		return "";
	}
}