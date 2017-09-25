
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"includeOrAttributeMappingOrTypeMapping"
})
@XmlRootElement(name = "transformations")
public class Transformations {

	@XmlElements({
		@XmlElement(name = "attribute-mapping", type = NamedAttributeMappingT.class),
		@XmlElement(name = "type-mapping", type = NamedTypeMappingT.class),
		@XmlElement(name = "transformation", type = TransformationT.class)
	})
	protected List<Object> includeOrAttributeMappingOrTypeMapping;

	public List<Object> getIncludeOrAttributeMappingOrTypeMapping() {
		if (this.includeOrAttributeMappingOrTypeMapping == null) {
			this.includeOrAttributeMappingOrTypeMapping = new ArrayList<>();
		}
		return this.includeOrAttributeMappingOrTypeMapping;
	}

}