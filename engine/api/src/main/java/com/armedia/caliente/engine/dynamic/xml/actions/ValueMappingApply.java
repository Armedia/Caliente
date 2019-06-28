/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.xml.CmfObjectArchetypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionApplyValueMapping.t", propOrder = {
	"comparison", "attributeName", "type", "mappingName", "cardinality", "fallback"
})
public class ValueMappingApply extends AbstractValueMappingApply<CmfObject.Archetype> {

	@XmlElement(name = "type", required = false)
	@XmlJavaTypeAdapter(CmfObjectArchetypeAdapter.class)
	protected CmfObject.Archetype type;

	@XmlElement(name = "mapping-name", required = false)
	protected Expression mappingName;

	public Expression getMappingName() {
		return this.mappingName;
	}

	public void setMappingName(Expression mappingName) {
		this.mappingName = mappingName;
	}

	@Override
	protected String getMappedLabel(DynamicElementContext ctx) throws ActionException {
		return Tools.toString(ActionTools.eval(getMappingName(), ctx));
	}

	@Override
	protected CmfObject.Archetype getMappingType(CmfObject.Archetype type) {
		return type;
	}

	@Override
	public void setType(CmfObject.Archetype type) {
		this.type = type;
	}

	@Override
	public CmfObject.Archetype getType() {
		return this.type;
	}
}