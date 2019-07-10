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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "childAssociation", propOrder = {
	"childName", "duplicate", "propagateTimestamps"
})
public class ChildAssociation extends Association {

	@XmlElement(name = "child-name")
	protected String childName;

	@XmlElement
	protected Boolean duplicate;

	@XmlElement
	protected Boolean propagateTimestamps;

	/**
	 * Gets the value of the childName property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getChildName() {
		return this.childName;
	}

	/**
	 * Sets the value of the childName property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setChildName(String value) {
		this.childName = value;
	}

	/**
	 * Gets the value of the duplicate property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getDuplicate() {
		return this.duplicate;
	}

	/**
	 * Sets the value of the duplicate property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setDuplicate(Boolean value) {
		this.duplicate = value;
	}

	/**
	 * Gets the value of the propagateTimestamps property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getPropagateTimestamps() {
		return this.propagateTimestamps;
	}

	/**
	 * Sets the value of the propagateTimestamps property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setPropagateTimestamps(Boolean value) {
		this.propagateTimestamps = value;
	}

}