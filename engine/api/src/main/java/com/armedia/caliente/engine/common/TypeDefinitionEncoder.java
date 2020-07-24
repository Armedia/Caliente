package com.armedia.caliente.engine.common;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.commons.enums.DateTimeFormat;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.JSONConverter;
import org.apache.chemistry.opencmis.commons.impl.XMLConstants;
import org.apache.chemistry.opencmis.commons.impl.XMLConverter;
import org.apache.chemistry.opencmis.commons.impl.XMLUtils;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.json.parser.JSONParseException;
import org.apache.chemistry.opencmis.commons.impl.json.parser.JSONParser;
import org.apache.commons.io.IOUtils;

import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.store.CmfEncodeableName;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.store.CmfValue.Type;
import com.armedia.commons.utilities.function.CheckedBiConsumer;
import com.armedia.commons.utilities.function.CheckedFunction;
import com.armedia.commons.utilities.function.LazySupplier;
import com.armedia.commons.utilities.io.TextMemoryBuffer;

public class TypeDefinitionEncoder {

	private static final LazySupplier<XMLOutputFactory> XML_OUTPUT_FACTORY = new LazySupplier<>(
		XMLOutputFactory::newInstance);
	private static final LazySupplier<XMLInputFactory> XML_INPUT_FACTORY = new LazySupplier<>(
		XMLInputFactory::newInstance);

	private static void writeToXML(TypeDefinition type, Writer out) throws XMLStreamException {
		XMLStreamWriter writer = null;
		try {
			writer = TypeDefinitionEncoder.XML_OUTPUT_FACTORY.get().createXMLStreamWriter(out);
			XMLUtils.startXmlDocument(writer);
			XMLConverter.writeTypeDefinition(writer, CmisVersion.CMIS_1_1, XMLConstants.NAMESPACE_CMIS, type);
			XMLUtils.endXmlDocument(writer);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	private static TypeDefinition readFromXML(Reader r) throws XMLStreamException {
		XMLStreamReader parser = TypeDefinitionEncoder.XML_INPUT_FACTORY.get().createXMLStreamReader(r);
		if (!XMLUtils.findNextStartElemenet(parser)) { return null; }
		TypeDefinition typeDef = XMLConverter.convertTypeDefinition(parser);
		parser.close();
		return typeDef;
	}

	private static void writeToJSON(TypeDefinition type, Writer out) throws IOException {
		JSONConverter.convert(type, DateTimeFormat.SIMPLE).writeJSONString(out);
		out.flush();
	}

	private static TypeDefinition readFromJSON(Reader r) throws JSONParseException, IOException {
		JSONParser parser = new JSONParser();
		Object json = parser.parse(r);
		if (!(json instanceof Map)) { throw new CmisRuntimeException("Invalid stream! Not a type definition!"); }
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) json;
		return JSONConverter.convertTypeDefinition(map);
	}

	private static <T> CmfProperty<T> encodeStringDefinition(TypeDefinition type, Function<String, T> encoder,
		CmfEncodeableName propertyName, CheckedBiConsumer<TypeDefinition, Writer, Exception> writer)
		throws ExportException {
		Reader bufIn = null;
		try (TextMemoryBuffer buf = new TextMemoryBuffer()) {
			writer.accept(type, buf);
			bufIn = buf.getReader();
		} catch (Exception e) {
			throw new ExportException("Failed to encode the XML formatted type definition for " + type.getId(), e);
		}

		CmfProperty<T> property = new CmfProperty<>(propertyName, Type.STRING, false);
		try (Reader r = bufIn) {
			property.setValue(encoder.apply(IOUtils.toString(r)));
		} catch (IOException e) {
			throw new ExportException("Unexpected IOException while working in memory", e);
		}
		return property;
	}

	public static <T> CmfProperty<T> encodeXmlDefinition(TypeDefinition type, Function<String, T> encoder)
		throws ExportException {
		return TypeDefinitionEncoder.encodeStringDefinition(type, encoder, IntermediateProperty.TYPE_DEFINITION_XML,
			TypeDefinitionEncoder::writeToXML);
	}

	public static <T> CmfProperty<T> encodeJsonDefinition(TypeDefinition type, Function<String, T> encoder)
		throws ExportException {
		return TypeDefinitionEncoder.encodeStringDefinition(type, encoder, IntermediateProperty.TYPE_DEFINITION_JSON,
			TypeDefinitionEncoder::writeToJSON);
	}

	public static <T> void encode(TypeDefinition type, CmfObject<T> object, Function<String, T> encoder)
		throws ExportException {
		TypeDefinitionEncoder.encode(type, object::setProperty, encoder);
	}

	public static <T> void encode(TypeDefinition type, Consumer<CmfProperty<T>> consumer, Function<String, T> encoder)
		throws ExportException {
		consumer.accept(TypeDefinitionEncoder.encodeXmlDefinition(type, encoder));
		consumer.accept(TypeDefinitionEncoder.encodeJsonDefinition(type, encoder));
	}

	private static <T> TypeDefinition decodeStringDefinition(CmfProperty<T> property, Function<T, String> decoder,
		CheckedFunction<Reader, TypeDefinition, Exception> reader) throws ImportException {
		String value = decoder.apply(property.getValue());
		try (Reader r = new StringReader(value)) {
			return reader.apply(r);
		} catch (IOException e) {
			throw new ImportException("Unexpected IOException working in memory", e);
		}
	}

	public static <T> TypeDefinition decodeXmlDefinition(CmfProperty<T> property, Function<T, String> decoder)
		throws ImportException {
		return TypeDefinitionEncoder.decodeStringDefinition(property, decoder, TypeDefinitionEncoder::readFromXML);
	}

	public static <T> TypeDefinition decodeJsonDefinition(CmfProperty<T> property, Function<T, String> decoder)
		throws ImportException {
		return TypeDefinitionEncoder.decodeStringDefinition(property, decoder, TypeDefinitionEncoder::readFromJSON);
	}

	public static <T> TypeDefinition decode(CmfObject<T> object, Function<T, String> decoder) throws ImportException {
		CmfProperty<T> property = object.getProperty(IntermediateProperty.TYPE_DEFINITION_JSON);
		if ((property != null) && property.hasValues()) {
			return TypeDefinitionEncoder.decodeJsonDefinition(property, decoder);
		}
		property = object.getProperty(IntermediateProperty.TYPE_DEFINITION_XML);
		if ((property != null) && property.hasValues()) {
			return TypeDefinitionEncoder.decodeXmlDefinition(property, decoder);
		}
		return null;
	}

	public static MutablePropertyDefinition<?> constructDefinition(PropertyType propertyType) {
		MutablePropertyDefinition<?> ret = null;
		switch (Objects.requireNonNull(propertyType, "Must provide the type of property to instantiate")) {
			case BOOLEAN:
				ret = new PropertyBooleanDefinitionImpl();
				break;

			case ID:
				ret = new PropertyIdDefinitionImpl();
				break;

			case INTEGER:
				ret = new PropertyIntegerDefinitionImpl();
				break;

			case DATETIME:
				ret = new PropertyDateTimeDefinitionImpl();
				break;

			case DECIMAL:
				ret = new PropertyDecimalDefinitionImpl();
				break;

			case HTML:
				ret = new PropertyHtmlDefinitionImpl();
				break;

			case STRING:
				ret = new PropertyStringDefinitionImpl();
				break;

			case URI:
				ret = new PropertyUriDefinitionImpl();
				break;

			default:
				break;
		}
		if (ret == null) {
			throw new IllegalArgumentException("The property type " + propertyType.name() + " is not yet supported");
		}
		ret.setPropertyType(propertyType);
		return ret;
	}
}