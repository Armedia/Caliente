
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeMapping.t", propOrder = {
	"setValueOrMapValue"
})
@XmlSeeAlso({
	NamedAttributeMappingT.class
})
public class AttributeMappingT {

	@XmlElements({
		@XmlElement(name = "set-value", type = SetValueT.class), @XmlElement(name = "map-value", type = MapValueT.class)
	})
	protected List<Transformation> setValueOrMapValue;

	public List<Transformation> getSetValueOrMapValue() {
		if (this.setValueOrMapValue == null) {
			this.setValueOrMapValue = new ArrayList<>();
		}
		return this.setValueOrMapValue;
	}
}