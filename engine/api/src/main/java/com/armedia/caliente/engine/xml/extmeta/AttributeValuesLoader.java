package com.armedia.caliente.engine.xml.extmeta;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.xml.ExternalMetadataContext;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

@XmlTransient
public interface AttributeValuesLoader {
	public <V> CmfAttribute<V> getAttributeValues(ExternalMetadataContext ctx, CmfObject<V> object) throws Exception;
}