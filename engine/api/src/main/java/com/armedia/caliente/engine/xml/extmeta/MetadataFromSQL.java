package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.xml.ExternalMetadataContext;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataFromSQL.t", propOrder = {
	"nameSources", "attributeQueries"
})
public class MetadataFromSQL implements AttributeValuesLoader {

	@XmlElementWrapper(name = "names")
	@XmlElements({
		@XmlElement(name = "list", type = SeparatedValuesList.class),
		@XmlElement(name = "query", type = MetadataNamesQuery.class)
	})
	protected List<AttributeNamesSource> nameSources;

	@XmlElement(name = "attribute-query", required = true)
	protected List<MetadataAttributeQuery> attributeQueries;

	public List<AttributeNamesSource> getNameSources() {
		if (this.nameSources == null) {
			this.nameSources = new ArrayList<>();
		}
		return this.nameSources;
	}

	public List<MetadataAttributeQuery> getAttributeQueries() {
		if (this.attributeQueries == null) {
			this.attributeQueries = new ArrayList<>();
		}
		return this.attributeQueries;
	}

	@Override
	public <V> CmfAttribute<V> getAttributeValues(ExternalMetadataContext ctx, CmfObject<V> object) throws Exception {
		return null;
	}

}