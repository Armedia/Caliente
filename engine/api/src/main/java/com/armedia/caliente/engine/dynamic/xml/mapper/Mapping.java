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
package com.armedia.caliente.engine.dynamic.xml.mapper;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import com.armedia.commons.utilities.Tools;

@XmlTransient
public abstract class Mapping extends MappingElement {

	public static final Character DEFAULT_SEP = ',';
	public static final String DEFAULT_SEP_STR = Mapping.DEFAULT_SEP.toString();

	@XmlAttribute(name = "tgt", required = true)
	protected String tgt;

	@XmlAttribute(name = "caseSensitive", required = false)
	protected Boolean caseSensitive;

	@XmlAttribute(name = "override", required = false)
	protected Boolean override;

	@XmlAttribute(name = "separator", required = false)
	protected String separator;

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		if (this.separator != null) {
			if (this.separator.length() > 1) {
				// Only use the first character
				this.separator = this.separator.substring(0, 1);
			} else {
				this.separator = null;
			}
		}
	}

	public String getTgt() {
		return this.tgt;
	}

	public void setTgt(String value) {
		this.tgt = value;
	}

	public boolean isOverride() {
		return Tools.coalesce(this.override, Boolean.FALSE);
	}

	public void setOverride(Boolean value) {
		this.override = value;
	}

	public boolean isCaseSensitive() {
		return Tools.coalesce(this.caseSensitive, Boolean.TRUE);
	}

	public void setCaseSensitive(Boolean value) {
		this.caseSensitive = value;
	}

	public char getSeparator() {
		if (this.separator == null) { return Mapping.DEFAULT_SEP; }
		return this.separator.charAt(0);
	}

	public void setSeparator(Character value) {
		if (value == null) {
			this.separator = null;
		} else {
			this.separator = value.toString();
		}
	}
}