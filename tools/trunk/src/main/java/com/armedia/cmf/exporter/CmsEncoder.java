/**
 *
 */

package com.armedia.cmf.exporter;

/**
 * @author diego
 *
 */
public interface CmsEncoder<T> {

	public String encode(String dataType, T value) throws CmsEncoderException;

}