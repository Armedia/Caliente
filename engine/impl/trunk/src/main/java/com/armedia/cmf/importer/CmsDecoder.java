/**
 *
 */

package com.armedia.cmf.importer;

/**
 * @author diego
 *
 */
public interface CmsDecoder<T> {

	public T decode(String dataType, String value) throws CmsDecoderException;

}