package com.armedia.caliente.engine.cmis.exporter;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyBooleanDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDateTimeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDecimalDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyIntegerDefinition;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyStringDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.junit.jupiter.api.Test;

public class CmisObjectTypeDelegateTest {

	@Test
	void testGetDefinitionValues() {
		Collection<PropertyDefinition<?>> definitions = new ArrayList<>();

		MutablePropertyBooleanDefinition pdBoolean = new PropertyBooleanDefinitionImpl();
		pdBoolean.setCardinality(null);
		pdBoolean.setDescription(null);
		pdBoolean.setDisplayName(null);
		pdBoolean.setId(null);
		pdBoolean.setIsInherited(null);
		pdBoolean.setIsOpenChoice(null);
		pdBoolean.setIsOrderable(null);
		pdBoolean.setIsQueryable(null);
		pdBoolean.setIsRequired(null);
		pdBoolean.setLocalName(null);
		pdBoolean.setLocalNamespace(null);
		pdBoolean.setPropertyType(PropertyType.BOOLEAN);
		pdBoolean.setQueryName(null);
		pdBoolean.setUpdatability(null);
		definitions.add(pdBoolean);

		MutablePropertyIntegerDefinition pdInteger = new PropertyIntegerDefinitionImpl();
		pdInteger.setCardinality(null);
		pdInteger.setDescription(null);
		pdInteger.setDisplayName(null);
		pdInteger.setId(null);
		pdInteger.setIsInherited(null);
		pdInteger.setIsOpenChoice(null);
		pdInteger.setIsOrderable(null);
		pdInteger.setIsQueryable(null);
		pdInteger.setIsRequired(null);
		pdInteger.setLocalName(null);
		pdInteger.setLocalNamespace(null);
		pdInteger.setPropertyType(PropertyType.INTEGER);
		pdInteger.setQueryName(null);
		pdInteger.setUpdatability(null);
		pdInteger.setMinValue(null);
		pdInteger.setMaxValue(null);
		definitions.add(pdInteger);

		MutablePropertyDecimalDefinition pdDecimal = new PropertyDecimalDefinitionImpl();
		pdDecimal.setCardinality(null);
		pdDecimal.setDescription(null);
		pdDecimal.setDisplayName(null);
		pdDecimal.setId(null);
		pdDecimal.setIsInherited(null);
		pdDecimal.setIsOpenChoice(null);
		pdDecimal.setIsOrderable(null);
		pdDecimal.setIsQueryable(null);
		pdDecimal.setIsRequired(null);
		pdDecimal.setLocalName(null);
		pdDecimal.setLocalNamespace(null);
		pdDecimal.setPropertyType(PropertyType.DECIMAL);
		pdDecimal.setQueryName(null);
		pdDecimal.setUpdatability(null);
		pdDecimal.setMinValue(null);
		pdDecimal.setMaxValue(null);
		pdDecimal.setPrecision(null);
		definitions.add(pdDecimal);

		MutablePropertyDateTimeDefinition pdDateTime = new PropertyDateTimeDefinitionImpl();
		pdDateTime.setCardinality(null);
		pdDateTime.setDescription(null);
		pdDateTime.setDisplayName(null);
		pdDateTime.setId(null);
		pdDateTime.setIsInherited(null);
		pdDateTime.setIsOpenChoice(null);
		pdDateTime.setIsOrderable(null);
		pdDateTime.setIsQueryable(null);
		pdDateTime.setIsRequired(null);
		pdDateTime.setLocalName(null);
		pdDateTime.setLocalNamespace(null);
		pdDateTime.setPropertyType(PropertyType.DATETIME);
		pdDateTime.setQueryName(null);
		pdDateTime.setUpdatability(null);
		definitions.add(pdDateTime);

		MutablePropertyStringDefinition pdString = new PropertyStringDefinitionImpl();
		pdString.setCardinality(null);
		pdString.setDescription(null);
		pdString.setDisplayName(null);
		pdString.setId(null);
		pdString.setIsInherited(null);
		pdString.setIsOpenChoice(null);
		pdString.setIsOrderable(null);
		pdString.setIsQueryable(null);
		pdString.setIsRequired(null);
		pdString.setLocalName(null);
		pdString.setLocalNamespace(null);
		pdString.setPropertyType(PropertyType.STRING);
		pdString.setQueryName(null);
		pdString.setUpdatability(null);
		pdString.setMaxLength(null);
		definitions.add(pdString);

		for (PropertyDefinition<?> def : definitions) {
			CmisObjectTypeDelegate.getDefinitionValues(def);
		}
	}

}