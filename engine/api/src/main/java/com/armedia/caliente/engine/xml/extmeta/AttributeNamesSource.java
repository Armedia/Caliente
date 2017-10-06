package com.armedia.caliente.engine.xml.extmeta;

import java.sql.Connection;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public interface AttributeNamesSource {

	public Set<String> getAttributeNames(Connection c) throws Exception;

}