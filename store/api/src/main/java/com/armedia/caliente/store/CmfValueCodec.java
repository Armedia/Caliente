/**
 *
 */

package com.armedia.caliente.store;

/**
 * @author diego
 *
 */
public interface CmfValueCodec<V> {

	public CmfValue encodeValue(V value);

	public V decodeValue(CmfValue value);

	public boolean isNull(V value);

	public V getNull();

}