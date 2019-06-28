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
package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.xml.Expression;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "parameterizedSQL.t", propOrder = {
	"sql", "skip", "count", "parameters"
})
public class ParameterizedQuery {

	@XmlElement(name = "sql", required = true)
	protected String sql;

	@XmlElement(name = "skip", required = false)
	protected Integer skip;

	@XmlElement(name = "count", required = false)
	protected Integer count;

	@XmlElement(name = "parameter", required = false)
	protected List<QueryParameter> parameters;

	public String getSql() {
		return this.sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public int getSkip() {
		if (this.skip == null) { return 0; }
		return Math.max(0, this.skip.intValue());
	}

	public void setSkip(Integer skip) {
		this.skip = skip;
	}

	public int getCount() {
		if (this.count == null) { return 0; }
		return Math.max(0, this.count.intValue());
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public List<QueryParameter> getParameters() {
		if (this.parameters == null) {
			this.parameters = new ArrayList<>();
		}
		return this.parameters;
	}

	public Map<String, Expression> getParameterMap() {
		Map<String, Expression> map = new HashMap<>();
		for (QueryParameter p : getParameters()) {
			String name = StringUtils.strip(p.getName());
			Expression value = p.getValue();
			if ((p != null) && !StringUtils.isEmpty(name)) {
				map.put(name, value);
			}
		}
		return map;
	}

}