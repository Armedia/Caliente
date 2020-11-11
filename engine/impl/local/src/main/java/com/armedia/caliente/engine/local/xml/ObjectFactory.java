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
package com.armedia.caliente.engine.local.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

	public static final String NS = "http://www.armedia.com/ns/caliente/engine/local";

	private final static QName _LocalQuerySearchList_QNAME = new QName(ObjectFactory.NS, "list");
	private final static QName _LocalQuerySearchDirectory_QNAME = new QName(ObjectFactory.NS, "directory");

	@XmlElementDecl(namespace = ObjectFactory.NS, name = "list", scope = LocalQuerySearch.class)
	public JAXBElement<String> createLocalQuerySearchList(String value) {
		return new JAXBElement<>(ObjectFactory._LocalQuerySearchList_QNAME, String.class, LocalQuerySearch.class,
			value);
	}

	@XmlElementDecl(namespace = ObjectFactory.NS, name = "directory", scope = LocalQuerySearch.class)
	public JAXBElement<String> createLocalQuerySearchDirectory(String value) {
		return new JAXBElement<>(ObjectFactory._LocalQuerySearchDirectory_QNAME, String.class, LocalQuerySearch.class,
			value);
	}
}