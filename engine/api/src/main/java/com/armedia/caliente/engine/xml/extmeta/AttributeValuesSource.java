package com.armedia.caliente.engine.xml.extmeta;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.xml.ExternalMetadataContext;

@XmlTransient
public interface AttributeValuesSource extends AttributeValuesLoader {

	public ExternalMetadataContext initialize() throws Exception;

	public void close(ExternalMetadataContext ctx);
}