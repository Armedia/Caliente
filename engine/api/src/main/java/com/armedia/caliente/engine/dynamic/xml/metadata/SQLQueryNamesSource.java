package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataNamesQuery.t", propOrder = {
	"value"
})
public class SQLQueryNamesSource extends AttributeNamesSource {

	@Override
	protected Set<String> getValues(Connection c) throws Exception {
		try (final Statement s = c.createStatement()) {
			try (final ResultSet rs = s.executeQuery(getValue())) {
				// Scan through the result set, and only take the values from the FIRST column
				Set<String> values = new HashSet<>();
				while (rs.next()) {
					String v = rs.getString(1);
					if (rs.wasNull()) {
						continue;
					}
					if (StringUtils.isEmpty(v)) {
						continue;
					}
					values.add(v);
				}
				return values;
			}
		}
	}

}