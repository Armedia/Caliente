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
package com.armedia.caliente.store.local;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.armedia.caliente.store.local.xml.legacy.StorePropertiesT;

public class XmlObjectFactory<P extends XmlProperty, S extends XmlStoreProperties<P>> {

	private final QName QNAME;

	private final Class<S> rootClass;

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.caliente.store.local.xml
	 *
	 */
	public XmlObjectFactory(String namespace, Class<S> rootClass) {
		this.QNAME = new QName(namespace, "store-properties");
		this.rootClass = rootClass;
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link StorePropertiesT }{@code >}
	 *
	 */
	protected final JAXBElement<S> createRoot(S value) {
		return new JAXBElement<>(this.QNAME, this.rootClass, null, value);
	}
}