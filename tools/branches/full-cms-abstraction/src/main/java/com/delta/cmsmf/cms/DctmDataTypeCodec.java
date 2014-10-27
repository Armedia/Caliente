/**
 *
 */

package com.delta.cmsmf.cms;

import com.armedia.cmf.storage.StoredValueCodec;
import com.armedia.cmf.storage.StoredValueDecoderException;
import com.armedia.cmf.storage.StoredValueEncoderException;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmDataTypeCodec implements StoredValueCodec<DctmDataType> {

	@Override
	public String encodeValue(DctmDataType value) throws StoredValueEncoderException {
		return value.name();
	}

	@Override
	public DctmDataType decodeValue(String value) throws StoredValueDecoderException {
		return DctmDataType.valueOf(value);
	}

}