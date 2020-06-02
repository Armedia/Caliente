package com.armedia.caliente.engine.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class TypeDataCodecTest {

	@Test
	public void testEncodeProperty() throws Exception {
		Collection<MutablePropertyDefinition<?>> original = new ArrayList<>();

		final Boolean[] booleanValues = {
			false, true
		};

		String[] strings = {
			UUID.randomUUID().toString(), UUID.randomUUID().toString()
		};

		// This is only for testing the common values
		for (PropertyType propertyType : PropertyType.values()) {
			for (Cardinality cardinality : Cardinality.values()) {
				for (Updatability updatability : Updatability.values()) {
					for (Boolean inherited : booleanValues) {
						for (Boolean orderable : booleanValues) {
							for (Boolean queryable : booleanValues) {
								for (Boolean required : booleanValues) {
									for (Boolean openChoice : booleanValues) {
										for (String id : strings) {
											for (String localNamespace : strings) {
												for (String localName : strings) {
													for (String queryName : strings) {
														for (String displayName : strings) {
															for (String description : strings) {
																MutablePropertyDefinition<?> pd = TypeDataCodec
																	.buildDefinition(propertyType);
																pd.setPropertyType(propertyType);
																pd.setCardinality(cardinality);
																pd.setUpdatability(updatability);
																pd.setIsInherited(inherited);
																pd.setIsOrderable(orderable);
																pd.setIsQueryable(queryable);
																pd.setIsRequired(required);
																pd.setIsOpenChoice(openChoice);
																id = "id-" + id;
																pd.setId(id);
																localNamespace = "localNamespace-" + localNamespace;
																pd.setLocalNamespace(localNamespace);
																localName = "localName" + localName;
																pd.setLocalName(localName);
																queryName = "queryName-" + queryName;
																pd.setQueryName(queryName);
																displayName = "displayName-" + displayName;
																pd.setDisplayName(displayName);
																description = "description-" + description;
																pd.setDescription(description);
																original.add(pd);
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		original.forEach(this::verifyValidity);
	}

	private void verifyValidity(PropertyDefinition<?> original) {
		Assertions.assertNotNull(original);

		PropertyDefinition<?> decoded = null;
		try {
			String str = TypeDataCodec.encodeProperty(original);
			decoded = TypeDataCodec.decodeProperty(str);
		} catch (JsonProcessingException e) {
			Assertions.fail(e);
		}

		Assertions.assertNotNull(decoded);
		Assertions.assertNotSame(original, decoded);
		Assertions.assertEquals(original.getCardinality(), decoded.getCardinality());
		Assertions.assertEquals(original.getClass(), decoded.getClass());
		Assertions.assertEquals(original.getDescription(), decoded.getDescription());
		Assertions.assertEquals(original.getDisplayName(), decoded.getDisplayName());
		Assertions.assertEquals(original.getId(), decoded.getId());
		Assertions.assertEquals(original.getLocalName(), decoded.getLocalName());
		Assertions.assertEquals(original.getLocalNamespace(), decoded.getLocalNamespace());
		Assertions.assertEquals(original.getPropertyType(), decoded.getPropertyType());
		Assertions.assertEquals(original.getQueryName(), decoded.getQueryName());
		Assertions.assertEquals(original.getUpdatability(), decoded.getUpdatability());
		Assertions.assertEquals(original.isInherited(), decoded.isInherited());
		Assertions.assertEquals(original.isOpenChoice(), decoded.isOpenChoice());
		Assertions.assertEquals(original.isOrderable(), decoded.isOrderable());
		Assertions.assertEquals(original.isQueryable(), decoded.isQueryable());
		Assertions.assertEquals(original.isRequired(), decoded.isRequired());
	}

	@Test
	public void testEncodeChoice() throws Exception {
	}

}
