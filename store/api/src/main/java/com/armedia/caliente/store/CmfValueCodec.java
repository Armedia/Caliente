/**
 *
 */

package com.armedia.caliente.store;

/**
 * @author diego
 *
 */
public interface CmfValueCodec<VALUE> {

	public CmfValue encodeValue(VALUE value);

	public VALUE decodeValue(CmfValue value);

	public boolean isNull(VALUE value);

	public VALUE getNull();

}