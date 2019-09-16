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

package com.armedia.caliente.engine.tools.xml;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueSerializer;
import com.armedia.caliente.store.xml.CmfValueTypeAdapter;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "metadataProperty.t", propOrder = {
	"values"
})
public class MetadataPropertyT {

	@XmlElement(name = "value", required = true)
	protected List<String> values;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "type", required = true)
	@XmlJavaTypeAdapter(CmfValueTypeAdapter.class)
	protected CmfValue.Type type;

	@XmlAttribute(name = "multivalue", required = false)
	protected boolean multivalue;

	public MetadataPropertyT() {

	}

	public MetadataPropertyT(CmfProperty<CmfValue> p) {
		this.name = p.getName();
		this.type = p.getType();
		this.multivalue = p.isMultivalued();
		CmfValueSerializer serializer = CmfValueSerializer.get(this.type);
		if (p.hasValues()) {
			this.values = new ArrayList<>(p.getValueCount());
			p.getValues().stream().map((t) -> {
				try {
					return serializer.encode(t);
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
			}).forEach(this.values::add);
		}
	}

	public List<String> getValues() {
		if (this.values == null) {
			this.values = new ArrayList<>();
		}
		return this.values;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CmfValue.Type getType() {
		return this.type;
	}

	public void setType(CmfValue.Type type) {
		this.type = type;
	}

	public boolean isMultivalue() {
		return this.multivalue;
	}

	public void setMultivalue(Boolean multivalue) {
		this.multivalue = Tools.coalesce(multivalue, Boolean.FALSE);
	}
}
