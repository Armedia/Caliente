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