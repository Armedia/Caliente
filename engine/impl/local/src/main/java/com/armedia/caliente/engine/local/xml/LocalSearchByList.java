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

import com.armedia.caliente.engine.local.exporter.LocalSearchType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "localSearchByList.t", propOrder = {
	"file", "encoding", "followLinks", "matching", "comment", "skip", "count", "postProcessors"
})
public class LocalSearchByList extends LocalSearchBase {

	@XmlElement(name = "file", required = true)
	protected String file;

	@XmlElement(name = "encoding", required = false)
	protected String encoding;

	@XmlElement(name = "followLinks", required = false)
	protected Boolean followLinks;

	@XmlElement(name = "matching", required = false)
	protected String matching;

	@XmlElement(name = "comment", required = false)
	protected String comment;

	@XmlElement(name = "skip", required = false)
	protected Integer skip;

	@XmlElement(name = "count", required = false)
	protected Integer count;

	public LocalSearchByList() {
		super(LocalSearchType.LIST);
	}

	public String getFile() {
		return this.file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
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

	public String getComment() {
		return this.comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Integer getSkip() {
		return this.skip;
	}

	public void setSkip(Integer skip) {
		this.skip = skip;
	}

	public Integer getCount() {
		return this.count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

}