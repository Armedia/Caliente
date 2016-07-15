package com.armedia.cmf.engine.alfresco.bulk.common;

import java.util.ArrayList;
import java.util.List;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.engine.tools.LocalOrganizationStrategy;
import com.armedia.cmf.storage.CmfAttributeTranslator;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfProperty;

public class AlfrescoBulkOrganizationStrategy extends LocalOrganizationStrategy {

	protected AlfrescoBulkOrganizationStrategy() {
		super("alfresco-bulk");
	}

	@Override
	protected List<String> calculatePath(CmfAttributeTranslator<?> translator, CmfObject<?> object) {
		List<String> path = new ArrayList<String>();
		CmfProperty<?> pathProp = object.getProperty(IntermediateProperty.PARENT_TREE_IDS);
		if (pathProp != null) {
			for (Object o : pathProp) {
				path.add(o.toString());
			}
			return path;
		}

		return super.calculatePath(translator, object);
	}
}