package com.armedia.cmf.storage;

import com.armedia.commons.utilities.PluggableServiceLocator;

public interface TypeDecoder {

	static final PluggableServiceLocator<TypeDecoder> DECODERS = new PluggableServiceLocator<TypeDecoder>(
		TypeDecoder.class);

	/**
	 * <p>
	 * Translate the given string to a valid, non-{@code null} {@link StoredObjectType} value, or
	 * return {@code null} if it can't translate it.
	 * </p>
	 *
	 * @param objectType
	 * @return a valid, non-{@code null} {@link StoredObjectType} value, or {@code null} if it can't
	 *         translate it
	 */

	public StoredObjectType translateObjectType(String objectType);

	/**
	 * <p>
	 * Translate the given string to a valid, non-{@code null} {@link StoredDataType} value, or
	 * return {@code null} if it can't translate it.
	 * </p>
	 *
	 * @param dataType
	 * @return a valid, non-{@code null} {@link StoredDataType} value, or {@code null} if it can't
	 *         translate it
	 */
	public StoredDataType translateDataType(String dataType);

}