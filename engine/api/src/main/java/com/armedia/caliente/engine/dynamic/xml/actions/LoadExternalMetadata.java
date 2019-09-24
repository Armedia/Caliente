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

package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataException;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueCodec;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionLoadExternalMetadata.t", propOrder = {
	"metadataSets"
})
public class LoadExternalMetadata extends ConditionalAction {

	@XmlElement(name = "metadata-set", required = false)
	protected List<ExternalMetadataSet> metadataSets;

	public List<ExternalMetadataSet> getMetadataSets() {
		if (this.metadataSets == null) {
			this.metadataSets = new ArrayList<>();
		}
		return this.metadataSets;
	}

	@Override
	protected void executeAction(DynamicElementContext<?> ctx) throws ActionException {
		executeActionTyped(ctx);
	}

	protected <V> void executeActionTyped(DynamicElementContext<V> ctx) throws ActionException {
		for (ExternalMetadataSet metadataSource : getMetadataSets()) {
			if (metadataSource == null) {
				continue;
			}

			String setName;
			try {
				setName = Tools.toString(Expression.eval(metadataSource, ctx));
			} catch (ScriptException e) {
				throw new ActionException(e);
			}

			if (StringUtils.isEmpty(setName)) {
				continue;
			}

			final boolean override = metadataSource.isOverride();
			final Map<String, CmfAttribute<V>> externalAttributes;
			try {
				externalAttributes = ctx.getAttributeValues(ctx.getBaseObject(), setName);
			} catch (ExternalMetadataException e) {
				throw new ActionException(
					String.format("Failed to load the external metadata for %s from metadata set [%s]",
						ctx.getBaseObject().getDescription(), setName),
					e);
			}

			final String varName = String.format("emdl:%s", setName);
			ctx.getVariables().put(varName,
				new DynamicValue(varName, CmfValue.Type.BOOLEAN, false).setValue(externalAttributes != null));
			if (externalAttributes == null) {
				// Nothing was loaded...
				continue;
			}

			Map<String, DynamicValue> currentAttributes = ctx.getDynamicObject().getAtt();
			CmfAttributeTranslator<V> translator = ctx.getBaseObject().getTranslator();
			for (String attributeName : externalAttributes.keySet()) {
				if (override || !currentAttributes.containsKey(attributeName)) {
					final CmfAttribute<V> external = externalAttributes.get(attributeName);
					final DynamicValue newAttribute = new DynamicValue(external, translator);
					currentAttributes.put(attributeName, newAttribute);
					final List<Object> newValues = new ArrayList<>(external.getValueCount());
					CmfValueCodec<V> codec = translator.getCodec(external.getType());
					for (V v : external) {
						newValues.add(codec.getValue(v));
					}
					newAttribute.setValues(newValues);
				}
			}
		}
	}

}