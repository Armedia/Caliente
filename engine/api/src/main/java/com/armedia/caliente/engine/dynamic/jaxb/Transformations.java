
package com.armedia.caliente.engine.dynamic.jaxb;

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

import com.armedia.caliente.engine.dynamic.Action;
import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.ObjectContext;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"elements"
})
@XmlRootElement(name = "transformations")
public final class Transformations extends XmlBase implements Action {

	@XmlElement(name = "transformation", type = ActionGroup.class)
	protected List<? extends Action> elements;

	public List<? extends Action> getElements() {
		if (this.elements == null) {
			this.elements = new ArrayList<>();
		}
		return this.elements;
	}

	@Override
	public void apply(ObjectContext ctx) throws ActionException {
		for (Action t : getElements()) {
			if (t != null) {
				t.apply(ctx);
			}
		}
	}

	public static Transformations loadFromXML(InputStream in) throws JAXBException {
		return XmlBase.loadFromXML(Transformations.class, in);
	}

	public static Transformations loadFromXML(Reader in) throws JAXBException {
		return XmlBase.loadFromXML(Transformations.class, in);
	}

	public static Transformations loadFromXML(XMLStreamReader in) throws JAXBException {
		return XmlBase.loadFromXML(Transformations.class, in);
	}
}