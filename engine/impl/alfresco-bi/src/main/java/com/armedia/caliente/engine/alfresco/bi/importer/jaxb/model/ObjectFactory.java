
package com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

	static final String NAMESPACE = "http://www.alfresco.org/model/dictionary/1.0";

	public static <T> List<T> getList(List<T> l) {
		if (l == null) { return new ArrayList<>(); }
		return l;
	}

}