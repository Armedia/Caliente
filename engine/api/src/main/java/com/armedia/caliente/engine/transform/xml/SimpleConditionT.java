
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

@XmlTransient
public abstract class SimpleConditionT implements Condition {

	@XmlValue
	protected String value;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}