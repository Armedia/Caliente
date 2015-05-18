/**
 *
 */

package com.armedia.cmf.storage;

/**
 * @author diego
 *
 */
public interface CmfValueCodec<V> {

	public CmfValue encodeValue(V value) throws CmfValueEncoderException;

	public V decodeValue(CmfValue value) throws CmfValueDecoderException;

	public boolean isNull(V value);

	public V getNull();

}