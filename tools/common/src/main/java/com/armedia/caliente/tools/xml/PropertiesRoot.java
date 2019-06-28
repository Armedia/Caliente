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
package com.armedia.caliente.tools.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "properties.t", propOrder = {
	"comment", "entries"
})
@XmlRootElement(name = "properties")
public class PropertiesRoot {

	@XmlElement(name = "entry")
	protected List<PropertiesEntry> entries;

	@XmlElement(name = "comment")
	protected PropertiesComment comment;

	public String getComment() {
		if (this.comment == null) { return null; }
		return this.comment.getValue();
	}

	public void setComment(String comment) {
		if (comment == null) {
			this.comment = null;
		} else {
			this.comment = new PropertiesComment();
			this.comment.setValue(comment);
		}
	}

	public List<PropertiesEntry> getEntries() {
		if (this.entries == null) {
			this.entries = new ArrayList<>();
		}
		return this.entries;
	}
}