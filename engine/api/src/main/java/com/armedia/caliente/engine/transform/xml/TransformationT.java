
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "transformation.t", propOrder = {
	"condition", "attributeMappingOrTypeMapping"
})
public class TransformationT {

	protected ConditionT condition;
	@XmlElements({
		@XmlElement(name = "attribute-mapping", type = AttributeMappingT.class),
		@XmlElement(name = "type-mapping", type = TypeMappingT.class)
	})
	protected List<Object> attributeMappingOrTypeMapping;

	public ConditionT getCondition() {
		return this.condition;
	}

	public void setCondition(ConditionT value) {
		this.condition = value;
	}

	public List<Object> getAttributeMappingOrTypeMapping() {
		if (this.attributeMappingOrTypeMapping == null) {
			this.attributeMappingOrTypeMapping = new ArrayList<>();
		}
		return this.attributeMappingOrTypeMapping;
	}

}