package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.util.Collections;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataNamesQuery.t", propOrder = {
	"query"
})
public class MetadataNamesQuery implements AttributeNamesSource {

	@XmlValue
	protected String query;

	public String getQuery() {
		return this.query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	@Override
	public Set<String> getAttributeNames(Connection c) throws Exception {
		// TODO get the JDBC connection, prepare the statement (if needed), and run it!
		return Collections.emptySet();
	}

}