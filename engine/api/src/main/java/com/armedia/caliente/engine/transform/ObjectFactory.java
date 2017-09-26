
package com.armedia.caliente.engine.transform;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {

	public static final String NAMESPACE = "http://www.armedia.com/ns/caliente/engine/transformations";

	private final static QName _ConditionGroupTIsReference_QNAME = new QName(ObjectFactory.NAMESPACE, "is-reference");
	private final static QName _ConditionGroupTIsCalientePropertyEmpty_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-caliente-property-empty");
	private final static QName _ConditionGroupTIsAttributeEmpty_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-attribute-empty");
	private final static QName _ConditionGroupTIsVariableSet_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-variable-set");
	private final static QName _ConditionGroupTIsSubtype_QNAME = new QName(ObjectFactory.NAMESPACE, "is-subtype");
	private final static QName _ConditionGroupTCheckExpression_QNAME = new QName(ObjectFactory.NAMESPACE,
		"check-expression");
	private final static QName _ConditionGroupTIsCalientePropertyValue_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-caliente-property-value");
	private final static QName _ConditionGroupTIsAttributeValue_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-attribute-value");
	private final static QName _ConditionGroupTNor_QNAME = new QName(ObjectFactory.NAMESPACE, "nor");
	private final static QName _ConditionGroupTNot_QNAME = new QName(ObjectFactory.NAMESPACE, "not");
	private final static QName _ConditionGroupTNand_QNAME = new QName(ObjectFactory.NAMESPACE, "nand");
	private final static QName _ConditionGroupTMux_QNAME = new QName(ObjectFactory.NAMESPACE, "mux");
	private final static QName _ConditionGroupTIsFirstVersion_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-first-version");
	private final static QName _ConditionGroupTXnor_QNAME = new QName(ObjectFactory.NAMESPACE, "xnor");
	private final static QName _ConditionGroupTXor_QNAME = new QName(ObjectFactory.NAMESPACE, "xor");
	private final static QName _ConditionGroupTIsAttributeRepeating_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-attribute-repeating");
	private final static QName _ConditionGroupTIsVariableValue_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-variable-value");
	private final static QName _ConditionGroupTCustomCheck_QNAME = new QName(ObjectFactory.NAMESPACE, "custom-check");
	private final static QName _ConditionGroupTOr_QNAME = new QName(ObjectFactory.NAMESPACE, "or");
	private final static QName _ConditionGroupTHasAttribute_QNAME = new QName(ObjectFactory.NAMESPACE, "has-attribute");
	private final static QName _ConditionGroupTHasDecorator_QNAME = new QName(ObjectFactory.NAMESPACE, "has-decorator");
	private final static QName _ConditionGroupTIsType_QNAME = new QName(ObjectFactory.NAMESPACE, "is-type");
	private final static QName _ConditionGroupTIsLatestVersion_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-latest-version");
	private final static QName _ConditionGroupTAnd_QNAME = new QName(ObjectFactory.NAMESPACE, "and");
	private final static QName _ConditionGroupTIsCalientePropertyRepeating_QNAME = new QName(ObjectFactory.NAMESPACE,
		"is-caliente-property-repeating");
	private final static QName _ConditionGroupTHasCalienteProperty_QNAME = new QName(ObjectFactory.NAMESPACE,
		"has-caliente-property");
	private final static QName _ConditionGroupTCustomScript_QNAME = new QName(ObjectFactory.NAMESPACE, "custom-script");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes
	 * for package: com.armedia.caliente.engine.transform.xml
	 *
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link Transformations }
	 *
	 */
	public Transformations createTransformations() {
		return new Transformations();
	}

	/**
	 * Create an instance of {@link TransformationT }
	 *
	 */
	public TransformationT createTransformationT() {
		return new TransformationT();
	}

	/**
	 * Create an instance of {@link ConditionIsAttributeEmptyT }
	 *
	 */
	public ConditionIsAttributeEmptyT createConditionIsAttributeEmptyT() {
		return new ConditionIsAttributeEmptyT();
	}

	/**
	 * Create an instance of {@link ActionSetSubtypeT }
	 *
	 */
	public ActionSetSubtypeT createActionSetSubtypeT() {
		return new ActionSetSubtypeT();
	}

	/**
	 * Create an instance of {@link ConditionCheckExpressionT }
	 *
	 */
	public ConditionCheckExpressionT createConditionCheckExpressionT() {
		return new ConditionCheckExpressionT();
	}

	/**
	 * Create an instance of {@link ConditionGroupXnorT }
	 *
	 */
	public ConditionGroupXnorT createConditionGroupXnorT() {
		return new ConditionGroupXnorT();
	}

	/**
	 * Create an instance of {@link ConditionIsLatestVersionT }
	 *
	 */
	public ConditionIsLatestVersionT createConditionIsLatestVersionT() {
		return new ConditionIsLatestVersionT();
	}

	/**
	 * Create an instance of {@link ActionClearVariableT }
	 *
	 */
	public ActionClearVariableT createActionClearVariableT() {
		return new ActionClearVariableT();
	}

	/**
	 * Create an instance of {@link ConditionIsVariableValueT }
	 *
	 */
	public ConditionIsVariableValueT createConditionIsVariableValueT() {
		return new ConditionIsVariableValueT();
	}

	/**
	 * Create an instance of {@link ConditionGroupNandT }
	 *
	 */
	public ConditionGroupNandT createConditionGroupNandT() {
		return new ConditionGroupNandT();
	}

	/**
	 * Create an instance of {@link ActionReplaceAttributeT }
	 *
	 */
	public ActionReplaceAttributeT createActionReplaceAttributeT() {
		return new ActionReplaceAttributeT();
	}

	/**
	 * Create an instance of {@link ConditionGroupAndT }
	 *
	 */
	public ConditionGroupAndT createConditionGroupAndT() {
		return new ConditionGroupAndT();
	}

	/**
	 * Create an instance of {@link ConditionIsCalientePropertyValueT }
	 *
	 */
	public ConditionIsCalientePropertyValueT createConditionIsCalientePropertyValueT() {
		return new ConditionIsCalientePropertyValueT();
	}

	/**
	 * Create an instance of {@link ConditionIsCalientePropertyRepeatingT }
	 *
	 */
	public ConditionIsCalientePropertyRepeatingT createConditionIsCalientePropertyRepeatingT() {
		return new ConditionIsCalientePropertyRepeatingT();
	}

	/**
	 * Create an instance of {@link ConditionGroupXorT }
	 *
	 */
	public ConditionGroupXorT createConditionGroupXorT() {
		return new ConditionGroupXorT();
	}

	/**
	 * Create an instance of {@link ConditionIsVariableSetT }
	 *
	 */
	public ConditionIsVariableSetT createConditionIsVariableSetT() {
		return new ConditionIsVariableSetT();
	}

	/**
	 * Create an instance of {@link ConditionGroupOrT }
	 *
	 */
	public ConditionGroupOrT createConditionGroupOrT() {
		return new ConditionGroupOrT();
	}

	/**
	 * Create an instance of {@link ActionReplaceDecoratorT }
	 *
	 */
	public ActionReplaceDecoratorT createActionReplaceDecoratorT() {
		return new ActionReplaceDecoratorT();
	}

	/**
	 * Create an instance of {@link ConditionGroupMuxT }
	 *
	 */
	public ConditionGroupMuxT createConditionGroupMuxT() {
		return new ConditionGroupMuxT();
	}

	/**
	 * Create an instance of {@link MapValueCaseT }
	 *
	 */
	public MapValueCaseT createMapValueCaseT() {
		return new MapValueCaseT();
	}

	/**
	 * Create an instance of {@link ConditionIsAttributeValueT }
	 *
	 */
	public ConditionIsAttributeValueT createConditionHasAttributeValueT() {
		return new ConditionIsAttributeValueT();
	}

	/**
	 * Create an instance of {@link ConditionIsCalientePropertyEmptyT }
	 *
	 */
	public ConditionIsCalientePropertyEmptyT createConditionIsCalientePropertyEmptyT() {
		return new ConditionIsCalientePropertyEmptyT();
	}

	/**
	 * Create an instance of {@link MapValueT }
	 *
	 */
	public MapValueT createMapValueT() {
		return new MapValueT();
	}

	/**
	 * Create an instance of {@link ExpressionT }
	 *
	 */
	public ExpressionT createExpressionT() {
		return new ExpressionT();
	}

	/**
	 * Create an instance of {@link ConditionIsAttributeRepeatingT }
	 *
	 */
	public ConditionIsAttributeRepeatingT createConditionIsAttributeRepeatingT() {
		return new ConditionIsAttributeRepeatingT();
	}

	/**
	 * Create an instance of {@link ConditionHasAttributeT }
	 *
	 */
	public ConditionHasAttributeT createConditionHasAttributeT() {
		return new ConditionHasAttributeT();
	}

	/**
	 * Create an instance of {@link ConditionGroupNorT }
	 *
	 */
	public ConditionGroupNorT createConditionGroupNorT() {
		return new ConditionGroupNorT();
	}

	/**
	 * Create an instance of {@link ActionAddDecoratorT }
	 *
	 */
	public ActionAddDecoratorT createActionAddDecoratorT() {
		return new ActionAddDecoratorT();
	}

	/**
	 * Create an instance of {@link ActionReplaceSubtypeT }
	 *
	 */
	public ActionReplaceSubtypeT createActionReplaceSubtypeT() {
		return new ActionReplaceSubtypeT();
	}

	/**
	 * Create an instance of {@link ActionCustomActionT }
	 *
	 */
	public ActionCustomActionT createActionCustomActionT() {
		return new ActionCustomActionT();
	}

	/**
	 * Create an instance of {@link ConditionHasCalientePropertyT }
	 *
	 */
	public ConditionHasCalientePropertyT createConditionHasCalientePropertyT() {
		return new ConditionHasCalientePropertyT();
	}

	/**
	 * Create an instance of {@link ActionRemoveAttributeT }
	 *
	 */
	public ActionRemoveAttributeT createActionRemoveAttributeT() {
		return new ActionRemoveAttributeT();
	}

	/**
	 * Create an instance of {@link ActionRemoveDecoratorT }
	 *
	 */
	public ActionRemoveDecoratorT createActionRemoveDecoratorT() {
		return new ActionRemoveDecoratorT();
	}

	/**
	 * Create an instance of {@link ConditionHasDecoratorT }
	 *
	 */
	public ConditionHasDecoratorT createConditionHasDecoratorT() {
		return new ConditionHasDecoratorT();
	}

	/**
	 * Create an instance of {@link ConditionIsFirstVersionT }
	 *
	 */
	public ConditionIsFirstVersionT createConditionIsFirstVersionT() {
		return new ConditionIsFirstVersionT();
	}

	/**
	 * Create an instance of {@link ActionMapAttributeValueT }
	 *
	 */
	public ActionMapAttributeValueT createActionMapAttributeValueT() {
		return new ActionMapAttributeValueT();
	}

	/**
	 * Create an instance of {@link ConditionIsReferenceT }
	 *
	 */
	public ConditionIsReferenceT createConditionIsReferenceT() {
		return new ConditionIsReferenceT();
	}

	/**
	 * Create an instance of {@link ActionSetAttributeT }
	 *
	 */
	public ActionSetAttributeT createActionSetAttributeT() {
		return new ActionSetAttributeT();
	}

	/**
	 * Create an instance of {@link ConditionIsTypeT }
	 *
	 */
	public ConditionIsTypeT createConditionTypeT() {
		return new ConditionIsTypeT();
	}

	/**
	 * Create an instance of {@link ConditionIsSubtypeT }
	 *
	 */
	public ConditionIsSubtypeT createConditionSubtypeT() {
		return new ConditionIsSubtypeT();
	}

	/**
	 * Create an instance of {@link ConditionGroupNotT }
	 *
	 */
	public ConditionGroupNotT createConditionGroupNotT() {
		return new ConditionGroupNotT();
	}

	/**
	 * Create an instance of {@link ActionSetVariableT }
	 *
	 */
	public ActionSetVariableT createActionSetVariableT() {
		return new ActionSetVariableT();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsReferenceT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-reference", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsReferenceT> createConditionGroupTIsReference(ConditionIsReferenceT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsReference_QNAME, ConditionIsReferenceT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsCalientePropertyEmptyT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-caliente-property-empty", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsCalientePropertyEmptyT> createConditionGroupTIsCalientePropertyEmpty(
		ConditionIsCalientePropertyEmptyT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsCalientePropertyEmpty_QNAME,
			ConditionIsCalientePropertyEmptyT.class, ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsAttributeEmptyT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-attribute-empty", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsAttributeEmptyT> createConditionGroupTIsAttributeEmpty(
		ConditionIsAttributeEmptyT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsAttributeEmpty_QNAME, ConditionIsAttributeEmptyT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsVariableSetT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-variable-set", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsVariableSetT> createConditionGroupTIsVariableSet(ConditionIsVariableSetT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsVariableSet_QNAME, ConditionIsVariableSetT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsSubtypeT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-subtype", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsSubtypeT> createConditionGroupTIsSubtype(ConditionIsSubtypeT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsSubtype_QNAME, ConditionIsSubtypeT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionCheckExpressionT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "check-expression", scope = ConditionGroupT.class)
	public JAXBElement<ConditionCheckExpressionT> createConditionGroupTCheckExpression(
		ConditionCheckExpressionT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTCheckExpression_QNAME, ConditionCheckExpressionT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsCalientePropertyValueT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-caliente-property-value", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsCalientePropertyValueT> createConditionGroupTIsCalientePropertyValue(
		ConditionIsCalientePropertyValueT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsCalientePropertyValue_QNAME,
			ConditionIsCalientePropertyValueT.class, ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsAttributeValueT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-attribute-value", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsAttributeValueT> createConditionGroupTIsAttributeValue(
		ConditionIsAttributeValueT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsAttributeValue_QNAME, ConditionIsAttributeValueT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionGroupNorT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "nor", scope = ConditionGroupT.class)
	public JAXBElement<ConditionGroupNorT> createConditionGroupTNor(ConditionGroupNorT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTNor_QNAME, ConditionGroupNorT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionGroupNotT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "not", scope = ConditionGroupT.class)
	public JAXBElement<ConditionGroupNotT> createConditionGroupTNot(ConditionGroupNotT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTNot_QNAME, ConditionGroupNotT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionGroupNandT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "nand", scope = ConditionGroupT.class)
	public JAXBElement<ConditionGroupNandT> createConditionGroupTNand(ConditionGroupNandT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTNand_QNAME, ConditionGroupNandT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionGroupMuxT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "mux", scope = ConditionGroupT.class)
	public JAXBElement<ConditionGroupMuxT> createConditionGroupTMux(ConditionGroupMuxT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTMux_QNAME, ConditionGroupMuxT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsFirstVersionT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-first-version", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsFirstVersionT> createConditionGroupTIsFirstVersion(ConditionIsFirstVersionT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsFirstVersion_QNAME, ConditionIsFirstVersionT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionGroupXnorT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "xnor", scope = ConditionGroupT.class)
	public JAXBElement<ConditionGroupXnorT> createConditionGroupTXnor(ConditionGroupXnorT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTXnor_QNAME, ConditionGroupXnorT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionGroupXorT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "xor", scope = ConditionGroupT.class)
	public JAXBElement<ConditionGroupXorT> createConditionGroupTXor(ConditionGroupXorT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTXor_QNAME, ConditionGroupXorT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsAttributeRepeatingT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-attribute-repeating", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsAttributeRepeatingT> createConditionGroupTIsAttributeRepeating(
		ConditionIsAttributeRepeatingT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsAttributeRepeating_QNAME,
			ConditionIsAttributeRepeatingT.class, ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsVariableValueT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-variable-value", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsVariableValueT> createConditionGroupTIsVariableValue(
		ConditionIsVariableValueT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsVariableValue_QNAME, ConditionIsVariableValueT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ExpressionT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "custom-check", scope = ConditionGroupT.class)
	public JAXBElement<ExpressionT> createConditionGroupTCustomCheck(ExpressionT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTCustomCheck_QNAME, ExpressionT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionGroupOrT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "or", scope = ConditionGroupT.class)
	public JAXBElement<ConditionGroupOrT> createConditionGroupTOr(ConditionGroupOrT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTOr_QNAME, ConditionGroupOrT.class, ConditionGroupT.class,
			value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionHasAttributeT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "has-attribute", scope = ConditionGroupT.class)
	public JAXBElement<ConditionHasAttributeT> createConditionGroupTHasAttribute(ConditionHasAttributeT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTHasAttribute_QNAME, ConditionHasAttributeT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionHasDecoratorT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "has-decorator", scope = ConditionGroupT.class)
	public JAXBElement<ConditionHasDecoratorT> createConditionGroupTHasDecorator(ConditionHasDecoratorT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTHasDecorator_QNAME, ConditionHasDecoratorT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsTypeT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-type", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsTypeT> createConditionGroupTIsType(ConditionIsTypeT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsType_QNAME, ConditionIsTypeT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionIsLatestVersionT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-latest-version", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsLatestVersionT> createConditionGroupTIsLatestVersion(
		ConditionIsLatestVersionT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsLatestVersion_QNAME, ConditionIsLatestVersionT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionGroupAndT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "and", scope = ConditionGroupT.class)
	public JAXBElement<ConditionGroupAndT> createConditionGroupTAnd(ConditionGroupAndT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTAnd_QNAME, ConditionGroupAndT.class,
			ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement
	 * }{@code <}{@link ConditionIsCalientePropertyRepeatingT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "is-caliente-property-repeating", scope = ConditionGroupT.class)
	public JAXBElement<ConditionIsCalientePropertyRepeatingT> createConditionGroupTIsCalientePropertyRepeating(
		ConditionIsCalientePropertyRepeatingT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTIsCalientePropertyRepeating_QNAME,
			ConditionIsCalientePropertyRepeatingT.class, ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ConditionHasCalientePropertyT
	 * }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "has-caliente-property", scope = ConditionGroupT.class)
	public JAXBElement<ConditionHasCalientePropertyT> createConditionGroupTHasCalienteProperty(
		ConditionHasCalientePropertyT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTHasCalienteProperty_QNAME,
			ConditionHasCalientePropertyT.class, ConditionGroupT.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link ExpressionT }{@code >}}
	 *
	 */
	@XmlElementDecl(namespace = ObjectFactory.NAMESPACE, name = "custom-script", scope = ConditionGroupT.class)
	public JAXBElement<ExpressionT> createConditionGroupTCustomScript(ExpressionT value) {
		return new JAXBElement<>(ObjectFactory._ConditionGroupTCustomScript_QNAME, ExpressionT.class,
			ConditionGroupT.class, value);
	}

}
