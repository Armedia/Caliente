package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.xml.ExternalMetadataContext;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadata.t", propOrder = {
	"sources"
})
public class Metadata implements AttributeValuesLoader {

	@XmlElements({
		@XmlElement(name = "from-sql", type = MetadataFromSQL.class),
		@XmlElement(name = "from-ddl", type = MetadataFromDDL.class)
	})
	protected List<AttributeValuesLoader> sources;

	public List<AttributeValuesLoader> getSources() {
		if (this.sources == null) {
			this.sources = new ArrayList<>();
		}
		return this.sources;
	}

	@Override
	public <V> CmfAttribute<V> getAttributeValues(ExternalMetadataContext ctx, CmfObject<V> object) throws Exception {
		// TODO: Go through each of the sources and load the attributes as required
		return null;
	}

}