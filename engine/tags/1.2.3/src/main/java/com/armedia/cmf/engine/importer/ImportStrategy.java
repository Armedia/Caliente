package com.armedia.cmf.engine.importer;

public interface ImportStrategy {

	public static enum BatchItemStrategy {
		/**
		 * Batches' contents must be processed serially, but multiple batches may be processed in
		 * parallel
		 */
		ITEMS_SERIALIZED,

		/**
		 * Batches' contents can be processed concurrently, but multiple batches should be processed
		 * serially
		 */
		ITEMS_CONCURRENT,
	}

	/**
	 * <p>
	 * Returns {@code true} if the strategy is to ignore this import, or {@code false} if this
	 * import should be processed normally.
	 * </p>
	 *
	 * @return {@code true} if the strategy is to ignore this import, or {@code false} if this
	 *         import should be processed normally.
	 */
	public boolean isIgnored();

	/**
	 * <p>
	 * Returns the mode of operation for processing batch contents, or {@code null} if batching
	 * should be ignored. This value is ignored if {@link #isParallelCapable()} returns
	 * {@code false}.
	 * </p>
	 *
	 * @return the mode of operation for processing batch contents, or {@code null} if batching
	 *         should be ignored.
	 */
	public BatchItemStrategy getBatchItemStrategy();

	/**
	 * <p>
	 * Returns {@code true} if batches are independent of each other, {@code false} otherwise. Note
	 * that it only makes sense for this method to return {@code true} if
	 * {@link #getBatchItemStrategy()} returns {@link BatchItemStrategy#ITEMS_CONCURRENT}, because
	 * only under that strategy does it make sense that the failure of one batch lead to not
	 * processing the next one(s).
	 * </p>
	 *
	 * @return {@code true} if batches are independent of each other, {@code false} otherwise
	 */
	public boolean isBatchIndependent();

	/**
	 * <p>
	 * Returns {@code true} if parallelization is supported, {@code false} otherwise.
	 * </p>
	 *
	 * @return {@code true} if parallelization is supported, {@code false} otherwise.
	 */
	public boolean isParallelCapable();

	/**
	 * <p>
	 * Returns {@code true} if batches should be failed after the first failure, or {@code false} if
	 * processing should continue regardless.
	 * </p>
	 *
	 * @return {@code true} if batches should be failed after the first failure, or {@code false} if
	 *         processing should continue regardless
	 */
	public boolean isBatchFailRemainder();

	/**
	 * <p>
	 * Returns {@code true} if transactions are supported for this object type, or {@code false}
	 * otherwise
	 * </p>
	 *
	 * @return {@code true} if transactions are supported for this object type, or {@code false}
	 *         otherwise
	 */
	public boolean isSupportsTransactions();
}