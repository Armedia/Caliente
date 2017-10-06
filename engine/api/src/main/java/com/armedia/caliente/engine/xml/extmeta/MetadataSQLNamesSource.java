package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataNames.t", propOrder = {
	"listOrQuery"
})
public class MetadataSQLNamesSource {

	@XmlElements({
		@XmlElement(name = "list", type = SeparatedValuesList.class),
		@XmlElement(name = "query", type = ExternalMetadataNamesQueryT.class)
	})
	protected List<Object> listOrQuery;

	public List<Object> getListOrQuery() {
		if (this.listOrQuery == null) {
			this.listOrQuery = new ArrayList<>();
		}
		return this.listOrQuery;
	}

}