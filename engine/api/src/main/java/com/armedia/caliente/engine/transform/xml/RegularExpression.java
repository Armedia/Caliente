
package com.armedia.caliente.engine.transform.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "regularExpression.t")
public class RegularExpression extends Expression {

	@XmlAttribute(name = "caseSensitive")
	protected Boolean caseSensitive;

	public boolean isCaseSensitive() {
		return Tools.coalesce(this.caseSensitive, Boolean.TRUE);
	}

	public void setCaseSensitive(Boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

}