package com.armedia.cmf.engine.importer;

public enum ContentsStrategy {
	// Batching should be ignored altogether
	IGNORE,

	// Batches' contents must be processed serially, but batches may be processed in parallel
	SERIALIZED,

	// Batches' contents can be parallelized, but batches should be processed serially
	PARALLEL,
}
