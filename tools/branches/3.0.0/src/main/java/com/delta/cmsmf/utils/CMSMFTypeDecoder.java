/**
 *
 */

package com.delta.cmsmf.utils;

import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.TypeDecoder;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CMSMFTypeDecoder implements TypeDecoder {

	@Override
	public StoredObjectType translateObjectType(String objectType) {
		if ("CONTENT".equals(objectType)) { return StoredObjectType.CONTENT_STREAM; }
		return null;
	}

	@Override
	public StoredDataType translateDataType(String dataType) {
		if (!dataType.startsWith("DF_")) { return null; }
		return StoredDataType.decodeString(dataType.substring(3));
	}
}