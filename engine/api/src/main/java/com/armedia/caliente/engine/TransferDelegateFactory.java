package com.armedia.caliente.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.commons.utilities.CfgTools;

public abstract class TransferDelegateFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, ?>, //
	ENGINE extends TransferEngine<SESSION, VALUE, CONTEXT, ?, ?, ?> //
> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ENGINE engine;
	private final CmfAttributeTranslator<VALUE> translator;
	private final CfgTools configuration;
	private final boolean skipRenditions;

	public TransferDelegateFactory(ENGINE engine, CfgTools configuration) {
		this.engine = engine;
		this.translator = engine.getTranslator();
		this.configuration = configuration;
		this.skipRenditions = configuration.getBoolean(TransferSetting.NO_RENDITIONS);
	}

	public final ENGINE getEngine() {
		return this.engine;
	}

	public final CfgTools getConfiguration() {
		return this.configuration;
	}

	public final CmfAttributeTranslator<VALUE> getTranslator() {
		return this.translator;
	}

	public final boolean isSkipRenditions() {
		return this.skipRenditions;
	}

	public void close() {
	}
}