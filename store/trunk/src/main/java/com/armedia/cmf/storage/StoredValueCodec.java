/**
 *
 */

package com.armedia.cmf.storage;

/**
 * @author diego
 *
 */
public interface StoredValueCodec<V> {

	public String encodeValue(V value) throws StoredValueEncoderException;

	public V decodeValue(String value) throws StoredValueDecoderException;

}