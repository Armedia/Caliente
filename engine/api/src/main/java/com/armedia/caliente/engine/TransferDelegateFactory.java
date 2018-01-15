package com.armedia.caliente.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.commons.utilities.CfgTools;

public abstract class TransferDelegateFactory<S, V, C extends TransferContext<S, V, ?>, E extends TransferEngine<S, V, C, ?, ?, ?>> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final E engine;
	private final CmfAttributeTranslator<V> translator;
	private final CfgTools configuration;
	private final boolean skipRenditions;

	public TransferDelegateFactory(E engine, CfgTools configuration) {
		this.engine = engine;
		this.translator = engine.getTranslator();
		this.configuration = configuration;
		this.skipRenditions = configuration.getBoolean(TransferSetting.NO_RENDITIONS);
	}

	public final E getEngine() {
		return this.engine;
	}

	public final CfgTools getConfiguration() {
		return this.configuration;
	}

	public final CmfAttributeTranslator<V> getTranslator() {
		return this.translator;
	}

	public final boolean isSkipRenditions() {
		return this.skipRenditions;
	}

	public void close() {
	}
}