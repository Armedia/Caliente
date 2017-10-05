
package com.armedia.caliente.engine.transform.xml;

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
	"filters"
})
@XmlRootElement(name = "filters")
public final class Filters extends XmlBase {

	@XmlElement(name = "filter", type = Filter.class)
	protected List<Action> filters;

	public List<Action> getFilters() {
		if (this.filters == null) {
			this.filters = new ArrayList<>();
		}
		return this.filters;
	}

	@Override
	public void apply(TransformationContext ctx) throws TransformationException {
		for (Action t : getFilters()) {
			if (t != null) {
				t.apply(ctx);
			}
		}
	}

	public static Filters loadFromXML(InputStream in) throws JAXBException {
		return XmlTools.unmarshal(Filters.class, in);
	}

	public static Filters loadFromXML(Reader in) throws JAXBException {
		return XmlTools.unmarshal(Filters.class, in);
	}

	public static Filters loadFromXML(XMLStreamReader in) throws JAXBException {
		return XmlTools.unmarshal(Filters.class, in);
	}
}