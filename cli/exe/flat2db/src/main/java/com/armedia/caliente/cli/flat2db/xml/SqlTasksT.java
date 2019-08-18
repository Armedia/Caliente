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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2019.05.01 at 11:04:18 AM CST
//

package com.armedia.caliente.cli.flat2db.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for sqlTasks.t complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="sqlTasks.t">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence maxOccurs="unbounded" minOccurs="0">
 *         &lt;choice>
 *           &lt;group ref="{http://www.armedia.com/ns/caliente/flat2db}sqlTasks"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sqlTasks.t", propOrder = {
	"sqlOrSqlScript"
})
public class SqlTasksT {

	@XmlElementRefs({
		@XmlElementRef(name = "sql", namespace = "http://www.armedia.com/ns/caliente/flat2db", type = JAXBElement.class, required = false),
		@XmlElementRef(name = "sql-script", namespace = "http://www.armedia.com/ns/caliente/flat2db", type = JAXBElement.class, required = false)
	})
	protected List<JAXBElement<String>> sqlOrSqlScript;

	/**
	 * Gets the value of the sqlOrSqlScript property.
	 *
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
	 * modification you make to the returned list will be present inside the JAXB object. This is
	 * why there is not a <CODE>set</CODE> method for the sqlOrSqlScript property.
	 *
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getSqlOrSqlScript().add(newItem);
	 * </pre>
	 *
	 *
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link JAXBElement
	 * }{@code <}{@link String }{@code >} {@link JAXBElement }{@code <}{@link String }{@code >}
	 *
	 *
	 */
	public List<JAXBElement<String>> getSqlOrSqlScript() {
		if (this.sqlOrSqlScript == null) {
			this.sqlOrSqlScript = new ArrayList<>();
		}
		return this.sqlOrSqlScript;
	}

}
