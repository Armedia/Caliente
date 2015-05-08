package com.armedia.cmf.engine.local.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.chemistry.opencmis.commons.PropertyIds;

import com.armedia.cmf.engine.converter.IntermediateProperty;
import com.armedia.cmf.storage.StoredAttribute;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredProperty;
import com.armedia.cmf.storage.URIStrategy;
import com.armedia.commons.utilities.FileNameTools;
import com.armedia.commons.utilities.Tools;

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
	protected String calculateSSP(StoredObject<?> object) {
		// Put it in the same path as it was in CMIS, but ensure each path component is
		// of a "universally-valid" format.
		StoredProperty<?> pathAtt = object.getProperty(IntermediateProperty.PATH.encode());
		StoredAttribute<?> nameAtt = object.getAttribute(PropertyIds.NAME);
		List<String> pathItems = new ArrayList<String>();
		if (pathAtt != null) {
			String path = "";
			if (pathAtt.hasValues()) {
				Object o = pathAtt.getValue();
				path = Tools.coalesce(o.toString(), path);
			}
			for (String s : FileNameTools.tokenize(path, '/')) {
				pathItems.add(encodeSafePathComponent(s));
			}
		}
		pathItems.add(encodeSafePathComponent(nameAtt.getValue().toString()));
		return FileNameTools.reconstitute(pathItems, true, false, '/');
	}
}