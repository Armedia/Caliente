/**
 *
 */

package com.delta.cmsmf.utils;

import com.armedia.cmf.storage.CmfDataType;
import com.armedia.cmf.storage.CmfTypeDecoder;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class CMSMFTypeDecoder extends CmfTypeDecoder {

	@Override
	public CmfDataType translateDataType(String dataType) {
		if (!dataType.startsWith("DF_")) { return null; }
		return CmfDataType.decodeString(dataType.substring(3));
	}
}