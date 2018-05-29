package com.armedia.caliente.store;

public interface CmfObjectHandler<VALUE> {
	/**
	 * <p>
	 * Signal the beginning of a new dependency tier, with the given number. Returns {@code true} if
	 * the tier should be processed, {@code false} if it should be skipped. If the tier is skipped,
	 * None of {@link #newHistory(String)}, {@link #handleObject(CmfObject)},
	 * {@link #endHistory(String, boolean)}, nor {@link #endTier(int, boolean)} will be invoked.
	 * </p>
	 *
	 * @param tierNumber
	 * @return {@code true} if the history should be processed, {@code false} if it should be
	 *         skipped
	 * @throws CmfStorageException
	 */
	public boolean newTier(int tierNumber) throws CmfStorageException;

	/**
	 * <p>
	 * Signal the beginning of a new history, with the given ID. Returns {@code true} if the history
	 * should be processed, {@code false} if it should be skipped. If the history is skipped,
	 * Neither {@link #endHistory(String, boolean)} nor {@link #handleObject(CmfObject)} will be
	 * invoked.
	 * </p>
	 *
	 * @param historyId
	 * @return {@code true} if the history should be processed, {@code false} if it should be
	 *         skipped
	 * @throws CmfStorageException
	 */
	public boolean newHistory(String historyId) throws CmfStorageException;

	/**
	 * <p>
	 * Handle the given object instance in the context of the currently-open history. This method
	 * should return {@code true} if the loop is to be continued, or {@code false} if no further
	 * attempt should be made to obtain objects.
	 * </p>
	 *
	 * @param dataObject
	 * @throws CmfStorageException
	 * @return {@code true} if more objects should be loaded, or {@code false} if this should be the
	 *         last object load attempted.
	 */
	public boolean handleObject(CmfObject<VALUE> dataObject) throws CmfStorageException;

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
	public boolean handleException(Exception e);

	/**
	 * <p>
	 * Close the current history, returning {@code true} if processing should continue with the next
	 * history, or {@code false} otherwise.
	 * </p>
	 *
	 * @param ok
	 *            {@code true} to indicate that the previous history was processed successfully,
	 *            {@code false} otherwise
	 * @return {@code true} if processing should continue with the next history, or {@code false}
	 *         otherwise
	 * @throws CmfStorageException
	 */
	public boolean endHistory(String historyId, boolean ok) throws CmfStorageException;

	/**
	 * <p>
	 * Close the current dependency tier, returning {@code true} if processing should continue with
	 * the next tier, or {@code false} otherwise.
	 * </p>
	 *
	 * @param ok
	 *            {@code true} to indicate that the previous tier was processed successfully,
	 *            {@code false} otherwise
	 * @return {@code true} if processing should continue with the next tier, or {@code false}
	 *         otherwise
	 * @throws CmfStorageException
	 */
	public boolean endTier(int tierNumber, boolean ok) throws CmfStorageException;
}