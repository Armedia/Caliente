package com.armedia.cmf.importer;

import com.armedia.cmf.storage.StoredObject;

public interface ImportListener {

	/**
	 * <p>
	 * Invoked when the import has started for the given object.
	 * </p>
	 *
	 * @param object
	 */
	public void objectImportStarted(StoredObject<?> object);

	/**
	 * <p>
	 * Invoked when the given object has been imported, and indicating the result of the import
	 * operation.
	 * </p>
	 *
	 * @param object
	 * @param cmsImportResult
	 */
	public void objectImportCompleted(StoredObject<?> object, ImportResult cmsImportResult, String newLabel,
		String newId);

	/**
	 * <p>
	 * Invoked when the import attempt on the given object has failed, and indicating the exception
	 * that was raised.
	 * </p>
	 *
	 * @param object
	 * @param thrown
	 */
	public void objectImportFailed(StoredObject<?> object, Throwable thrown);
}