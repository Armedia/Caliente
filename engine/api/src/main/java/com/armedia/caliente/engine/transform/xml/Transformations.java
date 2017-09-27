
package com.armedia.caliente.engine.transform.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"elements"
})
@XmlRootElement(name = "transformations")
public class Transformations implements Transformation {

	@XmlElement(name = "transformation", type = TransformationT.class)
	protected List<Transformation> elements;

	public List<Transformation> getElements() {
		if (this.elements == null) {
			this.elements = new ArrayList<>();
		}
		return this.elements;
	}

	@Override
	public <V> void apply(TransformationContext<V> ctx) {
		for (Transformation t : getElements()) {
			if (t != null) {
				t.apply(ctx);
			}
		}
	}

}