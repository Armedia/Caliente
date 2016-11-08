/**
 *
 */

package com.armedia.caliente.cli.caliente.utils;

import com.armedia.caliente.store.CmfDataType;
import com.armedia.caliente.store.CmfTypeDecoder;

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