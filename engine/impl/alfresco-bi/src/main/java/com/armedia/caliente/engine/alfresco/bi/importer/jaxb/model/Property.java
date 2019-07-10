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

package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "property", propOrder = {
	"title", "description", "type", "_protected", "mandatory", "multiple", "_default", "index", "constraints",
	"encrypted"
})
public class Property {

	@XmlElement
	protected String title;

	@XmlElement
	protected String description;

	@XmlElement(required = true)
	protected String type;

	@XmlElement(name = "protected")
	protected Boolean _protected;

	@XmlElement
	protected MandatoryDef mandatory;

	@XmlElement
	protected Boolean multiple;

	@XmlElement(name = "default")
	protected Object _default;

	@XmlElement
	protected Property.Index index;

	@XmlElementWrapper(name = "constraints")
	@XmlElement(name = "constraint")
	protected List<Constraint> constraints;

	@XmlElement
	protected Boolean encrypted;

	@XmlAttribute(required = true)
	protected String name;

	/**
	 * Gets the value of the title property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Sets the value of the title property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setTitle(String value) {
		this.title = value;
	}

	/**
	 * Gets the value of the description property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the value of the description property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setDescription(String value) {
		this.description = value;
	}

	/**
	 * Gets the value of the type property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Sets the value of the type property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setType(String value) {
		this.type = value;
	}

	/**
	 * Gets the value of the protected property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getProtected() {
		return this._protected;
	}

	/**
	 * Sets the value of the protected property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setProtected(Boolean value) {
		this._protected = value;
	}

	/**
	 * Gets the value of the mandatory property.
	 *
	 * @return possible object is {@link MandatoryDef }
	 *
	 */
	public MandatoryDef getMandatory() {
		return this.mandatory;
	}

	/**
	 * Sets the value of the mandatory property.
	 *
	 * @param value
	 *            allowed object is {@link MandatoryDef }
	 *
	 */
	public void setMandatory(MandatoryDef value) {
		this.mandatory = value;
	}

	/**
	 * Gets the value of the multiple property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getMultiple() {
		return this.multiple;
	}

	/**
	 * Sets the value of the multiple property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setMultiple(Boolean value) {
		this.multiple = value;
	}

	/**
	 * Gets the value of the default property.
	 *
	 * @return possible object is {@link Object }
	 *
	 */
	public Object getDefault() {
		return this._default;
	}

	/**
	 * Sets the value of the default property.
	 *
	 * @param value
	 *            allowed object is {@link Object }
	 *
	 */
	public void setDefault(Object value) {
		this._default = value;
	}

	/**
	 * Gets the value of the index property.
	 *
	 * @return possible object is {@link Property.Index }
	 *
	 */
	public Property.Index getIndex() {
		return this.index;
	}

	/**
	 * Sets the value of the index property.
	 *
	 * @param value
	 *            allowed object is {@link Property.Index }
	 *
	 */
	public void setIndex(Property.Index value) {
		this.index = value;
	}

	public List<Constraint> getConstraints() {
		return this.constraints = ObjectFactory.getList(this.constraints);
	}

	/**
	 * Gets the value of the encrypted property.
	 *
	 * @return possible object is {@link Boolean }
	 *
	 */
	public Boolean getEncrypted() {
		return this.encrypted;
	}

	/**
	 * Sets the value of the encrypted property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 *
	 */
	public void setEncrypted(Boolean value) {
		this.encrypted = value;
	}

	/**
	 * Gets the value of the name property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the value of the name property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setName(String value) {
		this.name = value;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"atomic", "stored", "tokenised", "facetable"
	})
	public static class Index {

		@XmlElement
		protected Boolean atomic;

		@XmlElement
		protected Boolean stored;

		@XmlElement
		protected String tokenised;

		@XmlElement
		protected Boolean facetable;

		@XmlAttribute(required = true)
		protected boolean enabled;

		/**
		 * Gets the value of the atomic property.
		 *
		 * @return possible object is {@link Boolean }
		 *
		 */
		public Boolean getAtomic() {
			return this.atomic;
		}

		/**
		 * Sets the value of the atomic property.
		 *
		 * @param value
		 *            allowed object is {@link Boolean }
		 *
		 */
		public void setAtomic(Boolean value) {
			this.atomic = value;
		}

		/**
		 * Gets the value of the stored property.
		 *
		 * @return possible object is {@link Boolean }
		 *
		 */
		public Boolean getStored() {
			return this.stored;
		}

		/**
		 * Sets the value of the stored property.
		 *
		 * @param value
		 *            allowed object is {@link Boolean }
		 *
		 */
		public void setStored(Boolean value) {
			this.stored = value;
		}

		/**
		 * Gets the value of the tokenised property.
		 *
		 * @return possible object is {@link String }
		 *
		 */
		public String getTokenised() {
			return this.tokenised;
		}

		/**
		 * Sets the value of the tokenised property.
		 *
		 * @param value
		 *            allowed object is {@link String }
		 *
		 */
		public void setTokenised(String value) {
			this.tokenised = value;
		}

		/**
		 * Gets the value of the facetable property.
		 *
		 * @return possible object is {@link Boolean }
		 *
		 */
		public Boolean getFacetable() {
			return this.facetable;
		}

		/**
		 * Sets the value of the facetable property.
		 *
		 * @param value
		 *            allowed object is {@link Boolean }
		 *
		 */
		public void setFacetable(Boolean value) {
			this.facetable = value;
		}

		/**
		 * Gets the value of the enabled property.
		 *
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets the value of the enabled property.
		 *
		 */
		public void setEnabled(boolean value) {
			this.enabled = value;
		}

	}

}