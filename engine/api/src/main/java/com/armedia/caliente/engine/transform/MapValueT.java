
package com.armedia.caliente.engine.transform;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mapValue.t", propOrder = {
	"cases", "defaultValue"
})
public class MapValueT {

	@XmlElement(name = "case", required = false)
	protected List<MapValueCaseT> cases;

	@XmlElement(name = "default", required = false)
	protected ExpressionT defaultValue;

	public List<MapValueCaseT> getCases() {
		if (this.cases == null) {
			this.cases = new ArrayList<>();
		}
		return this.cases;
	}

	public ExpressionT getDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(ExpressionT defaultValue) {
		this.defaultValue = defaultValue;
	}

}