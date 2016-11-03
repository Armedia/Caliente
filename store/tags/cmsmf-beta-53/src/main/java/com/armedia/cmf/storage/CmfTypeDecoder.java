package com.armedia.cmf.storage;

import com.armedia.commons.utilities.PluggableServiceLocator;

public class CmfTypeDecoder {

	static final PluggableServiceLocator<CmfTypeDecoder> DECODERS = new PluggableServiceLocator<CmfTypeDecoder>(
		CmfTypeDecoder.class);
	static {
		CmfTypeDecoder.DECODERS.setHideErrors(true);
	}

	/**
	 * <p>
	 * Translate the given string to a valid, non-{@code null} {@link CmfType} value, or return
	 * {@code null} if it can't translate it.
	 * </p>
	 *
	 * @param objectType
	 * @return a valid, non-{@code null} {@link CmfType} value, or {@code null} if it can't
	 *         translate it
	 */

	public CmfType translateObjectType(String objectType) {
		return null;
	}

	/**
	 * <p>
	 * Translate the given string to a valid, non-{@code null} {@link CmfDataType} value, or return
	 * {@code null} if it can't translate it.
	 * </p>
	 *
	 * @param dataType
	 * @return a valid, non-{@code null} {@link CmfDataType} value, or {@code null} if it can't
	 *         translate it
	 */
	public CmfDataType translateDataType(String dataType) {
		return null;
	}

}