
package com.armedia.cmf.engine.alfresco.bulk.importer.model.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each Java content interface and Java element interface
 * generated in the com.armedia.cmf.engine.alfresco.bulk.importer.model.jaxb package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation
 * for XML content. The Java representation of XML content can consist of schema derived interfaces
 * and classes representing the binding of schema type definitions, element declarations and model
 * groups. Factory methods for each of these are provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

	static final String NAMESPACE = "http://www.alfresco.org/model/dictionary/1.0";

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.cmf.engine.alfresco.bulk.importer.model.jaxb
	 *
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link ChildAssociation }
	 *
	 */
	public ChildAssociation createChildAssociation() {
		return new ChildAssociation();
	}

	/**
	 * Create an instance of {@link DataType }
	 *
	 */
	public DataType createModelDataTypesDataType() {
		return new DataType();
	}

	/**
	 * Create an instance of {@link Namespace }
	 *
	 */
	public Namespace createModelNamespacesNamespace() {
		return new Namespace();
	}

	/**
	 * Create an instance of {@link Association }
	 *
	 */
	public Association createAssociation() {
		return new Association();
	}

	/**
	 * Create an instance of {@link MandatoryDef }
	 *
	 */
	public MandatoryDef createMandatoryDef() {
		return new MandatoryDef();
	}

	/**
	 * Create an instance of {@link Property.Index }
	 *
	 */
	public Property.Index createPropertyIndex() {
		return new Property.Index();
	}

	/**
	 * Create an instance of {@link Association.Source }
	 *
	 */
	public Association.Source createAssociationSource() {
		return new Association.Source();
	}

	/**
	 * Create an instance of {@link Constraint }
	 *
	 */
	public Constraint createConstraint() {
		return new Constraint();
	}

	/**
	 * Create an instance of {@link PropertyOverride }
	 *
	 */
	public PropertyOverride createPropertyOverride() {
		return new PropertyOverride();
	}

	/**
	 * Create an instance of {@link Property }
	 *
	 */
	public Property createProperty() {
		return new Property();
	}

	/**
	 * Create an instance of {@link Association.Target }
	 *
	 */
	public Association.Target createAssociationTarget() {
		return new Association.Target();
	}

	/**
	 * Create an instance of {@link ClassElement.Associations }
	 *
	 */
	public ClassElement.Associations createClassAssociations() {
		return new ClassElement.Associations();
	}

	/**
	 * Create an instance of {@link Model }
	 *
	 */
	public Model createModel() {
		return new Model();
	}

	/**
	 * Create an instance of {@link Constraint.Parameter }
	 *
	 */
	public Constraint.Parameter createNamedValue() {
		return new Constraint.Parameter();
	}

	public static <T> List<T> getList(List<T> l) {
		if (l == null) { return new ArrayList<T>(); }
		return l;
	}
}