
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for expression.t complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="expression.t">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="lang" type="{http://www.armedia.com/ns/caliente/engine/transform}typeName.t" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "expression.t", propOrder = {
	"value"
})
@XmlSeeAlso({
	SwitchCaseMatchT.class
})
public class ExpressionT {

	@XmlValue
	protected String value;

	@XmlAttribute(name = "lang")
	protected String lang;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getLang() {
		return this.lang;
	}

	public void setLang(String value) {
		this.lang = value;
	}

	public <V> String evaluate(TransformationContext<V> context) {
		// TODO: Implement the script via JSR223
		return null;
	}

}