package com.armedia.cmf.engine.local.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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

	public LocalURIStrategy() {
		super("local");
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
		StoredProperty<?> paths = object.getProperty(IntermediateProperty.PATH.encode());
		String attName = translator.decodeAttributeName(type, IntermediateAttribute.NAME.encode());
		StoredAttribute<?> name = object.getAttribute(attName);

		String basePath = ((paths == null) || !paths.hasValues() ? "" : paths.getValue().toString());
		String baseName = name.getValue().toString();

		String finalPath = String.format("%s/%s", basePath, baseName);

		List<String> pathItems = new ArrayList<String>();
		for (String s : FileNameTools.tokenize(finalPath, '/')) {
			// TODO: Only fix the path if it needs fixing (i.e. for Windows)
			pathItems.add(encodeSafePathComponent(s));
		}
		return FileNameTools.reconstitute(pathItems, true, false, '/');
	}
}