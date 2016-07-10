
package com.armedia.cmf.engine.alfresco.bulk.importer.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * <p>
 * Java class for anonymous complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="author" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="published" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="imports" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="import" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}namespaceDefinition"/>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="namespaces">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="namespace" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}namespaceDefinition"/>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="data-types" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="data-type" maxOccurs="unbounded">
 *                     &lt;complexType>
 *                       &lt;complexContent>
 *                         &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                           &lt;sequence>
 *                             &lt;group ref="{http://www.alfresco.org/model/dictionary/1.0}TextualDescription"/>
 *                             &lt;element name="analyser-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *                             &lt;element name="java-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *                           &lt;/sequence>
 *                           &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}name"/>
 *                         &lt;/restriction>
 *                       &lt;/complexContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="constraints" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="constraint" type="{http://www.alfresco.org/model/dictionary/1.0}constraint" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="types" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="type" type="{http://www.alfresco.org/model/dictionary/1.0}type" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="aspects" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="aspect" type="{http://www.alfresco.org/model/dictionary/1.0}aspect" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}name"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"description", "author", "published", "version", "imports", "namespaces", "dataTypes", "constraints", "types",
	"aspects"
})
@XmlRootElement(name = "model")
public class Model {

	protected String description;
	protected String author;
	@XmlSchemaType(name = "date")
	protected XMLGregorianCalendar published;
	protected String version;
	protected Model.Imports imports;
	@XmlElement(required = true)
	protected Model.Namespaces namespaces;
	@XmlElement(name = "data-types")
	protected Model.DataTypes dataTypes;
	protected Model.Constraints constraints;
	protected Model.Types types;
	protected Model.Aspects aspects;
	@XmlAttribute(required = true)
	protected String name;

	/**
	 * Gets the value of the description property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the value of the description property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setDescription(String value) {
		this.description = value;
	}

	/**
	 * Gets the value of the author property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getAuthor() {
		return this.author;
	}

	/**
	 * Sets the value of the author property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setAuthor(String value) {
		this.author = value;
	}

	/**
	 * Gets the value of the published property.
	 *
	 * @return possible object is {@link XMLGregorianCalendar }
	 *
	 */
	public XMLGregorianCalendar getPublished() {
		return this.published;
	}

	/**
	 * Sets the value of the published property.
	 *
	 * @param value
	 *            allowed object is {@link XMLGregorianCalendar }
	 *
	 */
	public void setPublished(XMLGregorianCalendar value) {
		this.published = value;
	}

	/**
	 * Gets the value of the version property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Sets the value of the version property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setVersion(String value) {
		this.version = value;
	}

	/**
	 * Gets the value of the imports property.
	 *
	 * @return possible object is {@link Model.Imports }
	 *
	 */
	public Model.Imports getImports() {
		return this.imports;
	}

	/**
	 * Sets the value of the imports property.
	 *
	 * @param value
	 *            allowed object is {@link Model.Imports }
	 *
	 */
	public void setImports(Model.Imports value) {
		this.imports = value;
	}

	/**
	 * Gets the value of the namespaces property.
	 *
	 * @return possible object is {@link Model.Namespaces }
	 *
	 */
	public Model.Namespaces getNamespaces() {
		return this.namespaces;
	}

	/**
	 * Sets the value of the namespaces property.
	 *
	 * @param value
	 *            allowed object is {@link Model.Namespaces }
	 *
	 */
	public void setNamespaces(Model.Namespaces value) {
		this.namespaces = value;
	}

	/**
	 * Gets the value of the dataTypes property.
	 *
	 * @return possible object is {@link Model.DataTypes }
	 *
	 */
	public Model.DataTypes getDataTypes() {
		return this.dataTypes;
	}

	/**
	 * Sets the value of the dataTypes property.
	 *
	 * @param value
	 *            allowed object is {@link Model.DataTypes }
	 *
	 */
	public void setDataTypes(Model.DataTypes value) {
		this.dataTypes = value;
	}

	/**
	 * Gets the value of the constraints property.
	 *
	 * @return possible object is {@link Model.Constraints }
	 *
	 */
	public Model.Constraints getConstraints() {
		return this.constraints;
	}

	/**
	 * Sets the value of the constraints property.
	 *
	 * @param value
	 *            allowed object is {@link Model.Constraints }
	 *
	 */
	public void setConstraints(Model.Constraints value) {
		this.constraints = value;
	}

	/**
	 * Gets the value of the types property.
	 *
	 * @return possible object is {@link Model.Types }
	 *
	 */
	public Model.Types getTypes() {
		return this.types;
	}

	/**
	 * Sets the value of the types property.
	 *
	 * @param value
	 *            allowed object is {@link Model.Types }
	 *
	 */
	public void setTypes(Model.Types value) {
		this.types = value;
	}

	/**
	 * Gets the value of the aspects property.
	 *
	 * @return possible object is {@link Model.Aspects }
	 *
	 */
	public Model.Aspects getAspects() {
		return this.aspects;
	}

	/**
	 * Sets the value of the aspects property.
	 *
	 * @param value
	 *            allowed object is {@link Model.Aspects }
	 *
	 */
	public void setAspects(Model.Aspects value) {
		this.aspects = value;
	}

	/**
	 * Gets the value of the name property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the value of the name property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 *
	 * <p>
	 * The following schema fragment specifies the expected content contained within this class.
	 *
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;sequence>
	 *         &lt;element name="aspect" type="{http://www.alfresco.org/model/dictionary/1.0}aspect" maxOccurs="unbounded"/>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"aspect"
	})
	public static class Aspects {

		@XmlElement(required = true)
		protected List<Aspect> aspect;

		/**
		 * Gets the value of the aspect property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the aspect property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getAspect().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link Aspect }
		 *
		 *
		 */
		public List<Aspect> getAspect() {
			if (this.aspect == null) {
				this.aspect = new ArrayList<Aspect>();
			}
			return this.aspect;
		}

	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 *
	 * <p>
	 * The following schema fragment specifies the expected content contained within this class.
	 *
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;sequence>
	 *         &lt;element name="constraint" type="{http://www.alfresco.org/model/dictionary/1.0}constraint" maxOccurs="unbounded"/>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"constraint"
	})
	public static class Constraints {

		@XmlElement(required = true)
		protected List<Constraint> constraint;

		/**
		 * Gets the value of the constraint property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the constraint property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getConstraint().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link Constraint }
		 *
		 *
		 */
		public List<Constraint> getConstraint() {
			if (this.constraint == null) {
				this.constraint = new ArrayList<Constraint>();
			}
			return this.constraint;
		}

	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 *
	 * <p>
	 * The following schema fragment specifies the expected content contained within this class.
	 *
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;sequence>
	 *         &lt;element name="data-type" maxOccurs="unbounded">
	 *           &lt;complexType>
	 *             &lt;complexContent>
	 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *                 &lt;sequence>
	 *                   &lt;group ref="{http://www.alfresco.org/model/dictionary/1.0}TextualDescription"/>
	 *                   &lt;element name="analyser-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
	 *                   &lt;element name="java-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
	 *                 &lt;/sequence>
	 *                 &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}name"/>
	 *               &lt;/restriction>
	 *             &lt;/complexContent>
	 *           &lt;/complexType>
	 *         &lt;/element>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"dataType"
	})
	public static class DataTypes {

		@XmlElement(name = "data-type", required = true)
		protected List<Model.DataTypes.DataType> dataType;

		/**
		 * Gets the value of the dataType property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the dataType property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getDataType().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list
		 * {@link Model.DataTypes.DataType }
		 *
		 *
		 */
		public List<Model.DataTypes.DataType> getDataType() {
			if (this.dataType == null) {
				this.dataType = new ArrayList<Model.DataTypes.DataType>();
			}
			return this.dataType;
		}

		/**
		 * <p>
		 * Java class for anonymous complex type.
		 *
		 * <p>
		 * The following schema fragment specifies the expected content contained within this class.
		 *
		 * <pre>
		 * &lt;complexType>
		 *   &lt;complexContent>
		 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
		 *       &lt;sequence>
		 *         &lt;group ref="{http://www.alfresco.org/model/dictionary/1.0}TextualDescription"/>
		 *         &lt;element name="analyser-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
		 *         &lt;element name="java-class" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
		 *       &lt;/sequence>
		 *       &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}name"/>
		 *     &lt;/restriction>
		 *   &lt;/complexContent>
		 * &lt;/complexType>
		 * </pre>
		 *
		 *
		 */
		@XmlAccessorType(XmlAccessType.FIELD)
		@XmlType(name = "", propOrder = {
			"title", "description", "analyserClass", "javaClass"
		})
		public static class DataType {

			protected String title;
			protected String description;
			@XmlElement(name = "analyser-class")
			protected Object analyserClass;
			@XmlElement(name = "java-class")
			protected Object javaClass;
			@XmlAttribute(required = true)
			protected String name;

			/**
			 * Gets the value of the title property.
			 *
			 * @return possible object is {@link String }
			 *
			 */
			public String getTitle() {
				return this.title;
			}

			/**
			 * Sets the value of the title property.
			 *
			 * @param value
			 *            allowed object is {@link String }
			 *
			 */
			public void setTitle(String value) {
				this.title = value;
			}

			/**
			 * Gets the value of the description property.
			 *
			 * @return possible object is {@link String }
			 *
			 */
			public String getDescription() {
				return this.description;
			}

			/**
			 * Sets the value of the description property.
			 *
			 * @param value
			 *            allowed object is {@link String }
			 *
			 */
			public void setDescription(String value) {
				this.description = value;
			}

			/**
			 * Gets the value of the analyserClass property.
			 *
			 * @return possible object is {@link Object }
			 *
			 */
			public Object getAnalyserClass() {
				return this.analyserClass;
			}

			/**
			 * Sets the value of the analyserClass property.
			 *
			 * @param value
			 *            allowed object is {@link Object }
			 *
			 */
			public void setAnalyserClass(Object value) {
				this.analyserClass = value;
			}

			/**
			 * Gets the value of the javaClass property.
			 *
			 * @return possible object is {@link Object }
			 *
			 */
			public Object getJavaClass() {
				return this.javaClass;
			}

			/**
			 * Sets the value of the javaClass property.
			 *
			 * @param value
			 *            allowed object is {@link Object }
			 *
			 */
			public void setJavaClass(Object value) {
				this.javaClass = value;
			}

			/**
			 * Gets the value of the name property.
			 *
			 * @return possible object is {@link String }
			 *
			 */
			public String getName() {
				return this.name;
			}

			/**
			 * Sets the value of the name property.
			 *
			 * @param value
			 *            allowed object is {@link String }
			 *
			 */
			public void setName(String value) {
				this.name = value;
			}

		}

	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 *
	 * <p>
	 * The following schema fragment specifies the expected content contained within this class.
	 *
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;sequence>
	 *         &lt;element name="import" maxOccurs="unbounded">
	 *           &lt;complexType>
	 *             &lt;complexContent>
	 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *                 &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}namespaceDefinition"/>
	 *               &lt;/restriction>
	 *             &lt;/complexContent>
	 *           &lt;/complexType>
	 *         &lt;/element>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"_import"
	})
	public static class Imports {

		@XmlElement(name = "import", required = true)
		protected List<Model.Imports.Import> _import;

		/**
		 * Gets the value of the import property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the import property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getImport().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link Model.Imports.Import }
		 *
		 *
		 */
		public List<Model.Imports.Import> getImport() {
			if (this._import == null) {
				this._import = new ArrayList<Model.Imports.Import>();
			}
			return this._import;
		}

		/**
		 * <p>
		 * Java class for anonymous complex type.
		 *
		 * <p>
		 * The following schema fragment specifies the expected content contained within this class.
		 *
		 * <pre>
		 * &lt;complexType>
		 *   &lt;complexContent>
		 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
		 *       &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}namespaceDefinition"/>
		 *     &lt;/restriction>
		 *   &lt;/complexContent>
		 * &lt;/complexType>
		 * </pre>
		 *
		 *
		 */
		@XmlAccessorType(XmlAccessType.FIELD)
		@XmlType(name = "")
		public static class Import {

			@XmlAttribute(required = true)
			protected String uri;
			@XmlAttribute(required = true)
			protected String prefix;

			/**
			 * Gets the value of the uri property.
			 *
			 * @return possible object is {@link String }
			 *
			 */
			public String getUri() {
				return this.uri;
			}

			/**
			 * Sets the value of the uri property.
			 *
			 * @param value
			 *            allowed object is {@link String }
			 *
			 */
			public void setUri(String value) {
				this.uri = value;
			}

			/**
			 * Gets the value of the prefix property.
			 *
			 * @return possible object is {@link String }
			 *
			 */
			public String getPrefix() {
				return this.prefix;
			}

			/**
			 * Sets the value of the prefix property.
			 *
			 * @param value
			 *            allowed object is {@link String }
			 *
			 */
			public void setPrefix(String value) {
				this.prefix = value;
			}

		}

	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 *
	 * <p>
	 * The following schema fragment specifies the expected content contained within this class.
	 *
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;sequence>
	 *         &lt;element name="namespace" maxOccurs="unbounded">
	 *           &lt;complexType>
	 *             &lt;complexContent>
	 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *                 &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}namespaceDefinition"/>
	 *               &lt;/restriction>
	 *             &lt;/complexContent>
	 *           &lt;/complexType>
	 *         &lt;/element>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"namespace"
	})
	public static class Namespaces {

		@XmlElement(required = true)
		protected List<Model.Namespaces.Namespace> namespace;

		/**
		 * Gets the value of the namespace property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the namespace property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getNamespace().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list
		 * {@link Model.Namespaces.Namespace }
		 *
		 *
		 */
		public List<Model.Namespaces.Namespace> getNamespace() {
			if (this.namespace == null) {
				this.namespace = new ArrayList<Model.Namespaces.Namespace>();
			}
			return this.namespace;
		}

		/**
		 * <p>
		 * Java class for anonymous complex type.
		 *
		 * <p>
		 * The following schema fragment specifies the expected content contained within this class.
		 *
		 * <pre>
		 * &lt;complexType>
		 *   &lt;complexContent>
		 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
		 *       &lt;attGroup ref="{http://www.alfresco.org/model/dictionary/1.0}namespaceDefinition"/>
		 *     &lt;/restriction>
		 *   &lt;/complexContent>
		 * &lt;/complexType>
		 * </pre>
		 *
		 *
		 */
		@XmlAccessorType(XmlAccessType.FIELD)
		@XmlType(name = "")
		public static class Namespace {

			@XmlAttribute(required = true)
			protected String uri;
			@XmlAttribute(required = true)
			protected String prefix;

			/**
			 * Gets the value of the uri property.
			 *
			 * @return possible object is {@link String }
			 *
			 */
			public String getUri() {
				return this.uri;
			}

			/**
			 * Sets the value of the uri property.
			 *
			 * @param value
			 *            allowed object is {@link String }
			 *
			 */
			public void setUri(String value) {
				this.uri = value;
			}

			/**
			 * Gets the value of the prefix property.
			 *
			 * @return possible object is {@link String }
			 *
			 */
			public String getPrefix() {
				return this.prefix;
			}

			/**
			 * Sets the value of the prefix property.
			 *
			 * @param value
			 *            allowed object is {@link String }
			 *
			 */
			public void setPrefix(String value) {
				this.prefix = value;
			}

		}

	}

	/**
	 * <p>
	 * Java class for anonymous complex type.
	 *
	 * <p>
	 * The following schema fragment specifies the expected content contained within this class.
	 *
	 * <pre>
	 * &lt;complexType>
	 *   &lt;complexContent>
	 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
	 *       &lt;sequence>
	 *         &lt;element name="type" type="{http://www.alfresco.org/model/dictionary/1.0}type" maxOccurs="unbounded"/>
	 *       &lt;/sequence>
	 *     &lt;/restriction>
	 *   &lt;/complexContent>
	 * &lt;/complexType>
	 * </pre>
	 *
	 *
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
		"type"
	})
	public static class Types {

		@XmlElement(required = true)
		protected List<Type> type;

		/**
		 * Gets the value of the type property.
		 *
		 * <p>
		 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
		 * modification you make to the returned list will be present inside the JAXB object. This
		 * is why there is not a <CODE>set</CODE> method for the type property.
		 *
		 * <p>
		 * For example, to add a new item, do as follows:
		 *
		 * <pre>
		 * getType().add(newItem);
		 * </pre>
		 *
		 *
		 * <p>
		 * Objects of the following type(s) are allowed in the list {@link Type }
		 *
		 *
		 */
		public List<Type> getType() {
			if (this.type == null) {
				this.type = new ArrayList<Type>();
			}
			return this.type;
		}

	}

}