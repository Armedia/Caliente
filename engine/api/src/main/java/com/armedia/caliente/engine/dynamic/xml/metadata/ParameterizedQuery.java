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
	"sql", "parameters"
})
public class ParameterizedQuery {

	@XmlElement(name = "sql", required = true)
	protected String sql;

	@XmlElement(name = "parameter", required = false)
	protected List<QueryParameter> parameters;

	public String getSql() {
		return this.sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
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