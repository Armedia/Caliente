
package com.armedia.caliente.engine.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamReader;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.commons.utilities.XmlTools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"elements"
})
@XmlRootElement(name = "transformations")
public final class Transformations extends XmlBase {

	@XmlElement(name = "transformation", type = ActionGroup.class)
	protected List<Action> elements;

	public List<Action> getElements() {
		if (this.elements == null) {
			this.elements = new ArrayList<>();
		}
		return this.elements;
	}

	@Override
	public void apply(TransformationContext ctx) throws TransformationException {
		for (Action t : getElements()) {
			if (t != null) {
				t.apply(ctx);
			}
		}
	}

	public static Transformations loadFromXML(InputStream in) throws JAXBException {
		return XmlTools.unmarshal(Transformations.class, in);
	}

	public static Transformations loadFromXML(Reader in) throws JAXBException {
		return XmlTools.unmarshal(Transformations.class, in);
	}

	public static Transformations loadFromXML(XMLStreamReader in) throws JAXBException {
		return XmlTools.unmarshal(Transformations.class, in);
	}
}