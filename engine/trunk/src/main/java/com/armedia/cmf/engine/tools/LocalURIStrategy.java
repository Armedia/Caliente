package com.armedia.cmf.engine.tools;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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

	protected String encodeSafePathComponent(String pathComponent) {
		try {
			return URLEncoder.encode(pathComponent, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 encoding not supported in this JVM", e);
		}
	}

	@Override
	protected String calculateSSP(ObjectStorageTranslator<?> translator, StoredObject<?> object) {
		// Put it in the same path as it was in CMIS, but ensure each path component is
		// of a "universally-valid" format.
		final StoredObjectType type = object.getType();
		final StoredProperty<?> paths = object.getProperty(IntermediateProperty.PATH.encode());
		String attName = translator.decodeAttributeName(type, IntermediateAttribute.NAME.encode());
		final StoredAttribute<?> name = object.getAttribute(attName);
		attName = translator.decodeAttributeName(type, IntermediateAttribute.VERSION_LABEL.encode());
		final StoredAttribute<?> versionLabelAtt = object.getAttribute(attName);
		final String versionLabel;
		if ((versionLabelAtt != null) && versionLabelAtt.hasValues()) {
			versionLabel = versionLabelAtt.getValue().toString();
		} else {
			versionLabel = "";
		}

		String basePath = ((paths == null) || !paths.hasValues() ? "" : paths.getValue().toString());
		String baseName = name.getValue().toString();
		if (!StringUtils.isBlank(versionLabel)) {
			baseName = String.format("%s_%s", baseName, versionLabel);
		}

		String finalPath = String.format("%s/%s", basePath, baseName);

		List<String> pathItems = new ArrayList<String>();
		for (String s : FileNameTools.tokenize(finalPath, '/')) {
			// TODO: Only fix the path if it needs fixing (i.e. for Windows)
			pathItems.add(encodeSafePathComponent(s));
		}
		return FileNameTools.reconstitute(pathItems, true, false, '/');
	}
}