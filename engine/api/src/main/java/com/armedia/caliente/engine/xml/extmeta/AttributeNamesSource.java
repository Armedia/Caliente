package com.armedia.caliente.engine.xml.extmeta;

import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.caliente.engine.xml.ExternalMetadataContext;

@XmlTransient
public interface AttributeNamesSource {

	public Set<String> getAttributeNames(ExternalMetadataContext ctx) throws Exception;

}