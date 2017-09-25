
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
	"transformations"
})
@XmlSeeAlso({
	NamedAttributeMappingT.class
})
public class AttributeMappingT {

	@XmlElements({
		@XmlElement(name = "set-value", type = SetValueT.class), //
		@XmlElement(name = "remove-value", type = RemoveValueT.class), //
		@XmlElement(name = "map-value", type = MapValueT.class), //
	})
	protected List<Transformation> transformations;

	public List<Transformation> getTransformations() {
		if (this.transformations == null) {
			this.transformations = new ArrayList<>();
		}
		return this.transformations;
	}
}