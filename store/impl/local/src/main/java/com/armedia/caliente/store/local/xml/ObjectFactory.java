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
package com.armedia.caliente.store.local.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;

import com.armedia.caliente.store.local.XmlObjectFactory;

@XmlRegistry
public class ObjectFactory extends XmlObjectFactory<PropertyT, StorePropertiesT> {

	public static final String NAMESPACE = "http://www.armedia.com/ns/caliente/stores/local/store-properties";

	public ObjectFactory() {
		super(ObjectFactory.NAMESPACE, StorePropertiesT.class);
	}

	public StorePropertiesT createStorePropertiesT() {
		return new StorePropertiesT();
	}

	public PropertyT createPropertyT() {
		return new PropertyT();
	}

	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "store-properties")
	public JAXBElement<StorePropertiesT> createStoreProperties(StorePropertiesT value) {
		return super.createRoot(value);
	}
}