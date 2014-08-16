//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.08.15 at 05:58:25 PM CST 
//


package com.delta.cmsmf.io.xml;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.delta.cmsmf.io.xml package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Object_QNAME = new QName("http://www.armedia.com/ns/cmsmf/object", "object");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.delta.cmsmf.io.xml
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ObjectT }
     * 
     */
    public ObjectT createObjectT() {
        return new ObjectT();
    }

    /**
     * Create an instance of {@link ValueT }
     * 
     */
    public ValueT createValueT() {
        return new ValueT();
    }

    /**
     * Create an instance of {@link AttributeT }
     * 
     */
    public AttributeT createAttributeT() {
        return new AttributeT();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ObjectT }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.armedia.com/ns/cmsmf/object", name = "object")
    public JAXBElement<ObjectT> createObject(ObjectT value) {
        return new JAXBElement<ObjectT>(_Object_QNAME, ObjectT.class, null, value);
    }

}
