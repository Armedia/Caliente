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

package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mandatoryDef", propOrder = {
	"value"
})
public class MandatoryDef {

	@XmlAttribute
	protected Boolean enforced = Boolean.FALSE;

	@XmlValue
	protected String value = Boolean.FALSE.toString();

	/**
	 * Gets the value of the enforced property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getEnforced() {
		return this.enforced;
	}

	/**
	 * Sets the value of the enforced property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setEnforced(Boolean value) {
		this.enforced = Tools.coalesce(value, Boolean.FALSE);
	}

	public Boolean getValue() {
		return Boolean.valueOf(this.value);
	}

	public void setValue(Boolean value) {
		this.value = Tools.coalesce(value, Boolean.FALSE).toString();
	}
}