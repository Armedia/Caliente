
package com.armedia.caliente.engine.transform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "expression.t", propOrder = {
	"value"
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

	public <V> Object evaluate(TransformationContext<V> ctx) {
		// TODO: Evaluate the script as per JSR223
		return null;
	}
}