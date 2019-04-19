package com.armedia.caliente.engine.importer;

public interface ImportStrategy {

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
	public boolean isFailBatchOnError();

	/**
	 * <p>
	 * Returns {@code true} if this object type supports transactions, {@code false} otherwise.
	 * </p>
	 *
	 * @return {@code true} if this object type supports transactions, {@code false} otherwise.
	 */
	public boolean isSupportsTransactions();
}