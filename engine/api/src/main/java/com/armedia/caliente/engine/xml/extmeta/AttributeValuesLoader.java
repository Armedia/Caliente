package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

@XmlTransient
public interface AttributeValuesLoader {
	public <V> Map<String, CmfAttribute<V>> getAttributeValues(Connection connection, CmfObject<V> object)
		throws Exception;
}