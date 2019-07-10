/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;

public abstract class TransferDelegateFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends TransferContext<SESSION, VALUE, ?>, //
	ENGINE extends TransferEngine<?, ?, ?, SESSION, VALUE, CONTEXT, ?, ?, ?> //
> extends BaseShareableLockable {

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