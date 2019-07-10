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
package com.armedia.caliente.tools.alfresco.bi.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "scan.t", propOrder = {
	"items"
})
@XmlRootElement(name = "scan")
public class ScanIndex {
	@XmlElement(name = "item", required = true)
	protected List<ScanIndexItem> items;

	public List<ScanIndexItem> getItems() {
		if (this.items == null) {
			this.items = new ArrayList<>();
		}
		return this.items;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.items);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ScanIndex other = ScanIndex.class.cast(obj);
		if (!Tools.equals(this.items.size(), other.items.size())) { return false; }
		if (!Tools.equals(this.items, other.items)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("ScanIndex [items=%s]", this.items);
	}
}