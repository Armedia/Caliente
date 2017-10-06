package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataFromDDL.t", propOrder = {
	"query", "ignore", "transformDDLNames"
})
public class MetadataFromDDL implements AttributeValuesLoader {

	@XmlElement(name = "query", required = true)
	protected String query;

	@XmlElement(name = "ignore-columns", required = false)
	protected SeparatedValuesList ignore;

	@XmlElement(name = "transform-column-names", required = true)
	protected TransformDDLNames transformDDLNames;

	public String getQuery() {
		return this.query;
	}

	public void setQuery(String value) {
		this.query = value;
	}

	public SeparatedValuesList getIgnore() {
		return this.ignore;
	}

	public void setIgnore(SeparatedValuesList value) {
		this.ignore = value;
	}

	public TransformDDLNames getTransformNames() {
		return this.transformDDLNames;
	}

	public void setTransformNames(TransformDDLNames value) {
		this.transformDDLNames = value;
	}

	@Override
	public <V> Map<String, CmfAttribute<V>> getAttributeValues(Connection c, CmfObject<V> object) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}