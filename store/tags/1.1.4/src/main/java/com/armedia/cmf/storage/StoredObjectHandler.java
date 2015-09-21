package com.armedia.cmf.storage;

import java.sql.SQLException;

public interface StoredObjectHandler<V> {

	/**
	 * <p>
	 * Signal the beginning of a new batch, with the given ID. Returns {@code true} if the batch
	 * should be processed, {@code false} if it should be skipped. If the batch is skipped, Neither
	 * {@link #closeBatch(boolean)} nor {@link #handleObject(StoredObject)} will be invoked.
	 * </p>
	 *
	 * @param batchId
	 * @return {@code true} if the batch should be processed, {@code false} if it should be skipped
	 * @throws StorageException
	 */
	public boolean newBatch(String batchId) throws StorageException;

	/**
	 * <p>
	 * Handle the given object instance in the context of the currently-open batch. This method
	 * should return {@code true} if the loop is to be continued, or {@code false} if no further
	 * attempt should be made to obtain objects.
	 * </p>
	 *
	 * @param dataObject
	 * @throws StorageException
	 * @return {@code true} if more objects should be loaded, or {@code false} if this should be the
	 *         last object load attempted.
	 */
	public boolean handleObject(StoredObject<V> dataObject) throws StorageException;

	/**
	 * <p>
	 * Indicate that the load attempt failed for the object with the given ID, and provides the
	 * exception that describes the failure. It should return {@code true} if the code is expected
	 * to continue attempting to load objects, or {@code false} if the load attempt should be
	 * aborted.
	 * </p>
	 *
	 * @param e
	 * @return {@code true} if the load process should continue, {@code false} if it should be
	 *         aborted.
	 */
	public boolean handleException(SQLException e);

	/**
	 * <p>
	 * Close the current batch, returning {@code true} if processing should continue with the next
	 * batch, or {@code false} otherwise.
	 * </p>
	 *
	 * @param ok
	 *            {@code true} if processing should continue with the next batch, or {@code false}
	 *            otherwise
	 * @return {@code true} if processing should continue with the next batch, or {@code false}
	 *         otherwise
	 * @throws StorageException
	 */
	public boolean closeBatch(boolean ok) throws StorageException;
}