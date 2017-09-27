
package com.armedia.caliente.engine.transform.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.commons.utilities.XmlTools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"elements"
})
@XmlRootElement(name = "transformations")
public final class Transformations implements Action {

	private static String SCHEMA = "transformations.xsd";

	@XmlElement(name = "transformation", type = TransformationT.class)
	protected List<Action> elements;

	public List<Action> getElements() {
		if (this.elements == null) {
			this.elements = new ArrayList<>();
		}
		return this.elements;
	}

	@Override
	public <V> void apply(TransformationContext<V> ctx) {
		for (Action t : getElements()) {
			if (t != null) {
				t.apply(ctx);
			}
		}
	}

	public void storeToXML(OutputStream out) throws JAXBException {
		XmlTools.marshal(this, Transformations.SCHEMA, out);
	}

	public void storeToXML(Writer out) throws JAXBException {
		XmlTools.marshal(this, Transformations.SCHEMA, out);
	}

	public void storeToXML(XMLStreamWriter out) throws JAXBException {
		XmlTools.marshal(this, Transformations.SCHEMA, out);
	}

	public static Transformations loadFromXML(InputStream in) throws JAXBException {
		return XmlTools.unmarshal(Transformations.class, Transformations.SCHEMA, in);
	}

	public static Transformations loadFromXML(Reader in) throws JAXBException {
		return XmlTools.unmarshal(Transformations.class, Transformations.SCHEMA, in);
	}

	public static Transformations loadFromXML(XMLStreamReader in) throws JAXBException {
		return XmlTools.unmarshal(Transformations.class, Transformations.SCHEMA, in);
	}
}