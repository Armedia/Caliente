package com.armedia.cmf.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.CfgTools;

public abstract class TransferDelegateFactory<S, V, C extends TransferContext<S, V, ?>, E extends TransferEngine<S, V, C, ?, ?, ?>> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final E engine;
	private final CfgTools configuration;

	public TransferDelegateFactory(E engine, CfgTools configuration) {
		this.engine = engine;
		this.configuration = configuration;
	}

	public final E getEngine() {
		return this.engine;
	}

	public final CfgTools getConfiguration() {
		return this.configuration;
	}
}