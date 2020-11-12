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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.armedia.caliente.engine.local.exporter.LocalSearchType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "localSearchByPath.t", propOrder = {
	"path", "followLinks", "matching", "include", "maxDepth", "postProcessors"
})
public class LocalSearchByPath extends LocalSearchBase {

	@XmlElement(name = "path", required = true)
	protected String path;

	@XmlElement(name = "followLinks", required = false)
	protected Boolean followLinks;

	@XmlElement(name = "matching", required = false)
	protected String matching;

	@XmlJavaTypeAdapter(LocalIncludeModeAdapter.class)
	@XmlElement(name = "include", required = false)
	protected LocalIncludeMode include;

	@XmlElement(name = "maxDepth", required = false)
	protected Integer maxDepth;

	public LocalSearchByPath() {
		super(LocalSearchType.PATH);
	}

	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Boolean getFollowLinks() {
		return this.followLinks;
	}

	public void setFollowLinks(Boolean followLinks) {
		this.followLinks = followLinks;
	}

	public String getMatching() {
		return this.matching;
	}

	public void setMatching(String matching) {
		this.matching = matching;
	}

	public LocalIncludeMode getInclude() {
		return this.include;
	}

	public void setInclude(LocalIncludeMode include) {
		this.include = include;
	}

	public Integer getMaxDepth() {
		return this.maxDepth;
	}

	public void setMaxDepth(Integer maxDepth) {
		this.maxDepth = maxDepth;
	}
}