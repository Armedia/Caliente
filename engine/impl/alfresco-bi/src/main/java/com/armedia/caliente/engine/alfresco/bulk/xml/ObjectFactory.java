
package com.armedia.caliente.engine.alfresco.bulk.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each Java content interface and Java element interface
 * generated in the com.armedia.caliente.engine.alfresco.bulk.importer.model.jaxb package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation
 * for XML content. The Java representation of XML content can consist of schema derived interfaces
 * and classes representing the binding of schema type definitions, element declarations and model
 * groups. Factory methods for each of these are provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

	static final String NAMESPACE = "http://java.sun.com/dtd/properties.dtd";

	public ObjectFactory() {
	}

	public PropertiesComment createPropertiesComment() {
		return new PropertiesComment();
	}

	public PropertiesEntry createPropertiesEntry() {
		return new PropertiesEntry();
	}

	public PropertiesRoot createPropertiesRoot() {
		return new PropertiesRoot();
	}

	public static <T> List<T> getList(List<T> l) {
		if (l == null) { return new ArrayList<>(); }
		return l;
	}
}