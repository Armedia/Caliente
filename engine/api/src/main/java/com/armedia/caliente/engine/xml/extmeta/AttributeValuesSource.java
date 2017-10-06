package com.armedia.caliente.engine.xml.extmeta;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface AttributeValuesSource {

	public void initialize() throws Exception;

	public void close();
}