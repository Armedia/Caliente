package com.armedia.cmf.engine.tools;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.OrganizationStrategy;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.commons.utilities.FileNameTools;

public class LocalOrganizationStrategy extends OrganizationStrategy {

	public static final String NAME = "localfs";

	public LocalOrganizationStrategy() {
		super(LocalOrganizationStrategy.NAME);
	}

	@Override
	public String calculateAddendum(ObjectStorageTranslator<?> translator, StoredObject<?> object, String qualifier) {
		final String attName = translator.decodeAttributeName(object.getType(),
			IntermediateAttribute.VERSION_LABEL.encode());
		final StoredAttribute<?> versionLabelAtt = object.getAttribute(attName);
		String oldFrag = super.calculateAddendum(translator, object, qualifier);
		if ((versionLabelAtt != null) && versionLabelAtt.hasValues()) {
			final String versionLabel = versionLabelAtt.getValue().toString();
			if (StringUtils.isBlank(versionLabel)) { return oldFrag; }
			if (StringUtils.isBlank(oldFrag)) { return versionLabel; }
			return String.format("%s_%s", oldFrag, versionLabel);
		}
		return oldFrag;
	}

	@Override
	protected List<String> calculatePath(ObjectStorageTranslator<?> translator, StoredObject<?> object) {
		// Put it in the same path as it was in CMIS, but ensure each path component is
		// of a "universally-valid" format.
		StoredProperty<?> paths = object.getProperty(IntermediateProperty.PATH.encode());

		List<String> ret = new ArrayList<String>();
		for (String p : FileNameTools.tokenize(paths.getValue().toString(), '/')) {
			ret.add(p);
		}

		StoredAttribute<?> name = object.getAttribute(translator.decodeAttributeName(object.getType(),
			IntermediateAttribute.NAME.encode()));
		ret.add(name.getValue().toString());
		return ret;
	}
}