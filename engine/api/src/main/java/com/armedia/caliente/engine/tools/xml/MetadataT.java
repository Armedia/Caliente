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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.xml.CmfObjectArchetypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "metadata.t", propOrder = {
	"archetype", "id", "searchKey", "number", "name", "parentIds", "dependencyTier", "historyId", "historyCurrent",
	"subtype", "secondarySubtypes", "attributes", "properties"
})
@XmlRootElement(name = "metadata")
public class MetadataT {

	@XmlElement(name = "archetype", required = true)
	@XmlSchemaType(name = "string")
	@XmlJavaTypeAdapter(CmfObjectArchetypeAdapter.class)
	protected CmfObject.Archetype archetype;

	@XmlElement(name = "id", required = true)
	protected String id;

	@XmlElement(name = "searchKey", required = true)
	protected String searchKey;

	@XmlElement(name = "number", required = true)
	@XmlSchemaType(name = "nonNegativeInteger")
	protected Long number;

	@XmlElement(name = "name", required = true)
	protected String name;

	@XmlElementWrapper(name = "parents")
	@XmlElement(name = "ref", required = true)
	protected List<ObjectRefT> parentIds;

	@XmlElement(name = "dependencyTier", required = true)
	@XmlSchemaType(name = "nonNegativeInteger")
	protected Integer dependencyTier;

	@XmlElement(name = "historyId", required = true)
	protected String historyId;

	@XmlElement(name = "historyCurrent", required = false)
	protected boolean historyCurrent;

	@XmlElement(name = "subtype", required = true)
	protected String subtype;

	@XmlElementWrapper(name = "secondarySubtypes", required = false)
	@XmlElement(name = "subtype", required = true)
	protected List<String> secondarySubtypes;

	@XmlElementWrapper(name = "attributes", required = false)
	@XmlElement(name = "entry", required = true)
	protected List<MetadataPropertyT> attributes;

	@XmlElementWrapper(name = "cmfProperties", required = false)
	@XmlElement(name = "entry", required = true)
	protected List<MetadataPropertyT> properties;

	public MetadataT() {

	}

	public MetadataT(CmfObject<CmfValue> o) {
		this.archetype = o.getType();
		this.id = o.getId();
		this.searchKey = o.getSearchKey();
		this.number = o.getNumber();
		this.name = o.getName();
		if (!o.getParentReferences().isEmpty()) {
			this.parentIds = new ArrayList<>(o.getParentReferences().size());
			o.getParentReferences().stream().map((r) -> new ObjectRefT(r)).forEach(this.parentIds::add);
		}
		this.dependencyTier = o.getDependencyTier();
		this.historyId = o.getHistoryId();
		this.historyCurrent = o.isHistoryCurrent();
		this.subtype = o.getSubtype();
		if (!o.getSecondarySubtypes().isEmpty()) {
			this.secondarySubtypes = new ArrayList<>(o.getSecondarySubtypes());
		}
		if (o.getAttributeCount() > 0) {
			this.attributes = new ArrayList<>(o.getAttributeCount());
			o.getAttributes().stream().map((a) -> new MetadataPropertyT(a)).forEach(this.attributes::add);
		}
		if (o.getPropertyCount() > 0) {
			this.properties = new ArrayList<>(o.getPropertyCount());
			o.getProperties().stream().map((p) -> new MetadataPropertyT(p)).forEach(this.properties::add);
		}
	}

	protected <T> List<T> getList(List<T> l) {
		if (l == null) { return new ArrayList<>(); }
		return l;
	}

	public CmfObject.Archetype getArchetype() {
		return this.archetype;
	}

	public void setArchetype(CmfObject.Archetype archetype) {
		this.archetype = archetype;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSearchKey() {
		return this.searchKey;
	}

	public void setSearchKey(String searchKey) {
		this.searchKey = searchKey;
	}

	public Long getNumber() {
		return this.number;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ObjectRefT> getParentIds() {
		return this.parentIds = getList(this.parentIds);
	}

	public Integer getDependencyTier() {
		return this.dependencyTier;
	}

	public void setDependencyTier(Integer dependencyTier) {
		this.dependencyTier = dependencyTier;
	}

	public String getHistoryId() {
		return this.historyId;
	}

	public void setHistoryId(String historyId) {
		this.historyId = historyId;
	}

	public boolean isHistoryCurrent() {
		return this.historyCurrent;
	}

	public void setHistoryCurrent(boolean historyCurrent) {
		this.historyCurrent = historyCurrent;
	}

	public String getSubtype() {
		return this.subtype;
	}

	public void setSubtype(String subtype) {
		this.subtype = subtype;
	}

	public List<String> getSecondarySubtypes() {
		return this.secondarySubtypes = getList(this.secondarySubtypes);
	}

	public List<MetadataPropertyT> getAttributes() {
		return this.attributes = getList(this.attributes);
	}

	public List<MetadataPropertyT> getProperties() {
		return this.properties = getList(this.properties);
	}
}