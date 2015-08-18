/**
 *
 */

package com.delta.cmsmf.utils;

import com.armedia.cmf.storage.StoredDataType;
import com.armedia.cmf.storage.TypeDecoder;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CMSMFTypeDecoder extends TypeDecoder {

	@Override
	public StoredDataType translateDataType(String dataType) {
		if (!dataType.startsWith("DF_")) { return null; }
		return StoredDataType.decodeString(dataType.substring(3));
	}
}