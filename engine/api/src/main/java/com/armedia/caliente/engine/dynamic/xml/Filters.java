package com.armedia.caliente.engine.dynamic.xml;

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
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.XMLStreamReader;

import com.armedia.caliente.engine.dynamic.xml.filter.Filter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"filters", "defaultOutcome"
})
@XmlRootElement(name = "filters")
@XmlSchema("engine.xsd")
public class Filters {

	@XmlElement(name = "filter", required = false)
	protected List<Filter> filters;

	@XmlElement(name = "default", required = false)
	@XmlJavaTypeAdapter(FilterOutcomeAdapter.class)
	protected FilterOutcome defaultOutcome;

	public List<Filter> getFilters() {
		if (this.filters == null) {
			this.filters = new ArrayList<>();
		}
		return this.filters;
	}

	public FilterOutcome getDefaultOutcome() {
		return this.defaultOutcome;
	}

	public void setDefaultOutcome(FilterOutcome defaultOutcome) {
		this.defaultOutcome = defaultOutcome;
	}

	public static Filters loadFromXML(InputStream in) throws JAXBException {
		return XmlBase.loadFromXML(Filters.class, in);
	}

	public static Filters loadFromXML(Reader in) throws JAXBException {
		return XmlBase.loadFromXML(Filters.class, in);
	}

	public static Filters loadFromXML(XMLStreamReader in) throws JAXBException {
		return XmlBase.loadFromXML(Filters.class, in);
	}
}