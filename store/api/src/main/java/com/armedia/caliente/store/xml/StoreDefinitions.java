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
package com.armedia.caliente.store.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "stores.t", propOrder = {
	"setting", "objectstore", "contentstore"
})
@XmlRootElement(name = "stores")
public class StoreDefinitions extends SettingContainer {

	@XmlElement(required = true)
	protected List<StoreConfiguration> objectstore;

	@XmlElement(required = true)
	protected List<StoreConfiguration> contentstore;

	@Override
	protected void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		super.afterUnmarshal(unmarshaller, parent);
	}

	@Override
	protected void beforeMarshal(Marshaller marshaller) {
		super.beforeMarshal(marshaller);
	}

	public List<StoreConfiguration> getObjectStores() {
		if (this.objectstore == null) {
			this.objectstore = new ArrayList<>();
		}
		return this.objectstore;
	}

	public List<StoreConfiguration> getContentStores() {
		if (this.contentstore == null) {
			this.contentstore = new ArrayList<>();
		}
		return this.contentstore;
	}

	@Override
	public StoreDefinitions clone() {
		StoreDefinitions newClone = StoreDefinitions.class.cast(super.clone());
		if (this.objectstore != null) {
			newClone.objectstore = new ArrayList<>(this.objectstore);
		}
		if (this.contentstore != null) {
			newClone.contentstore = new ArrayList<>(this.contentstore);
		}
		return newClone;
	}
}