package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

@XmlTransient
public interface AttributeValuesLoader {
	public void initialize(Connection c) throws Exception;

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(Connection connection, CmfObject<V> object)
		throws Exception;

	public String getDataSource();

	public void close();
}