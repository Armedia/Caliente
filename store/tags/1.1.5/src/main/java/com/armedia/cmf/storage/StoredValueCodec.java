/**
 *
 */

package com.armedia.cmf.storage;

/**
 * @author diego
 *
 */
public interface StoredValueCodec<V> {

	public StoredValue encodeValue(V value) throws StoredValueEncoderException;

	public V decodeValue(StoredValue value) throws StoredValueDecoderException;

	public boolean isNull(V value);

	public V getNull();

}