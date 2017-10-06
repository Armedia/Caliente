package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadata.t", propOrder = {
	"fromSqlOrFromDdl"
})
public class Metadata {

	@XmlElements({
		@XmlElement(name = "from-sql", type = ExternalMetadataFromSQLT.class),
		@XmlElement(name = "from-ddl", type = ExternalMetadataFromDDLT.class)
	})
	protected List<Object> fromSqlOrFromDdl;

	public List<Object> getFromSqlOrFromDdl() {
		if (this.fromSqlOrFromDdl == null) {
			this.fromSqlOrFromDdl = new ArrayList<>();
		}
		return this.fromSqlOrFromDdl;
	}

}