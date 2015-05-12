package com.armedia.cmf.engine.tools;

import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.engine.converter.IntermediateAttribute;
import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.URIStrategy;
import com.armedia.commons.utilities.FileNameTools;

public class LocalURIStrategy extends URIStrategy {

	public static final String NAME = "localfs";

	public LocalURIStrategy() {
		super(LocalURIStrategy.NAME);
	}

	@Override
	public String calculateFragment(ObjectStorageTranslator<?> translator, StoredObject<?> object, String qualifier) {
		final String attName = translator.decodeAttributeName(object.getType(),
			IntermediateAttribute.VERSION_LABEL.encode());
		final StoredAttribute<?> versionLabelAtt = object.getAttribute(attName);
		String oldFrag = super.calculateFragment(translator, object, qualifier);
		if ((versionLabelAtt != null) && versionLabelAtt.hasValues()) {
			final String versionLabel = versionLabelAtt.getValue().toString();
			if (StringUtils.isBlank(versionLabel)) { return oldFrag; }
			if (StringUtils.isBlank(oldFrag)) { return versionLabel; }
			return String.format("%s_%s", oldFrag, versionLabel);
		}
		return oldFrag;
	}

	@Override
	protected String calculateSSP(ObjectStorageTranslator<?> translator, StoredObject<?> object) {
		// Put it in the same path as it was in CMIS, but ensure each path component is
		// of a "universally-valid" format.
		final StoredObjectType type = object.getType();
		final StoredProperty<?> paths = object.getProperty(IntermediateProperty.PATH.encode());
		final StoredAttribute<?> name = object.getAttribute(translator.decodeAttributeName(type,
			IntermediateAttribute.NAME.encode()));
		String basePath = ((paths == null) || !paths.hasValues() ? "" : paths.getValue().toString());
		String finalPath = String.format("%s/%s", basePath, name.getValue().toString());
		return FileNameTools.reconstitute(FileNameTools.tokenize(finalPath, '/'), true, false, '/');
	}
}