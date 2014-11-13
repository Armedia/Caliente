package com.armedia.cmf.engine.importer;

public interface ImportStrategy {

	public static enum BatchingStrategy {
		/** Batches' contents must be processed serially, but batches may be processed in parallel */
		SERIALIZED,

		/** Batches' contents can be parallelized, but batches should be processed serially */
		PARALLEL,
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
	public BatchingStrategy getBatchingStrategy();

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
}