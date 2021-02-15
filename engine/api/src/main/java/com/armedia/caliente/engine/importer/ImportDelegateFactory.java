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
package com.armedia.caliente.engine.importer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.armedia.caliente.engine.TransferDelegateFactory;
import com.armedia.caliente.engine.common.SessionWrapper;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public abstract class ImportDelegateFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, ?>, //
	ENGINE extends ImportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?, ?>//
> extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ENGINE> {

	private final Map<CmfValue.Type, VALUE> nullValues;

	protected ImportDelegateFactory(ENGINE engine, CfgTools configuration) {
		super(engine, configuration);

		final CmfAttributeTranslator<VALUE> translator = getTranslator();
		final Map<CmfValue.Type, VALUE> nullValues = new EnumMap<>(CmfValue.Type.class);
		for (CmfValue.Type t : CmfValue.Type.values()) {
			CmfValueCodec<VALUE> codec = translator.getCodec(t);
			VALUE v = codec.decode(t.getNull());
			nullValues.put(t, v);
		}
		this.nullValues = Tools.freezeMap(nullValues);
	}

	protected final VALUE getAttributeValue(CmfObject<VALUE> cmfObject, CmfEncodeableName attribute) {
		return getAttributeValue(cmfObject, attribute.encode());
	}

	protected final VALUE getAttributeValue(CmfObject<VALUE> cmfObject, String attribute) {
		CmfAttribute<VALUE> att = cmfObject.getAttribute(attribute);
		if (att == null) { return this.nullValues.get(CmfValue.Type.OTHER); }
		if (att.hasValues()) { return att.getValue(); }
		return this.nullValues.get(att.getType());
	}

	protected final List<VALUE> getAttributeValues(CmfObject<VALUE> cmfObject, CmfEncodeableName attribute) {
		return getAttributeValues(cmfObject, attribute.encode());
	}

	protected final List<VALUE> getAttributeValues(CmfObject<VALUE> cmfObject, String attribute) {
		CmfAttribute<VALUE> att = cmfObject.getAttribute(attribute);
		if (att == null) { return Collections.emptyList(); }
		return att.getValues();
	}

	protected final VALUE getPropertyValue(CmfObject<VALUE> cmfObject, CmfEncodeableName attribute) {
		return getPropertyValue(cmfObject, attribute.encode());
	}

	protected final VALUE getPropertyValue(CmfObject<VALUE> cmfObject, String attribute) {
		CmfProperty<VALUE> prop = cmfObject.getProperty(attribute);
		if (prop == null) { return this.nullValues.get(CmfValue.Type.OTHER); }
		if (prop.hasValues()) { return prop.getValue(); }
		return this.nullValues.get(prop.getType());
	}

	protected final List<VALUE> getPropertyValues(CmfObject<VALUE> cmfObject, CmfEncodeableName attribute) {
		return getPropertyValues(cmfObject, attribute.encode());
	}

	protected final List<VALUE> getPropertyValues(CmfObject<VALUE> cmfObject, String attribute) {
		CmfProperty<VALUE> att = cmfObject.getProperty(attribute);
		if (att == null) { return Collections.emptyList(); }
		return att.getValues();
	}

	protected abstract ImportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ENGINE> newImportDelegate(
		CmfObject<VALUE> storedObject) throws Exception;
}