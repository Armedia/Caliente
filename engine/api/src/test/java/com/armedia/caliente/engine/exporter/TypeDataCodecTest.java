package com.armedia.caliente.engine.exporter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.DateTimeResolution;
import org.apache.chemistry.opencmis.commons.enums.DecimalPrecision;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class TypeDataCodecTest {

	public class MPD<T> extends AbstractPropertyDefinition<T> {
		private static final long serialVersionUID = 1L;

		private void copyTo(MutablePropertyDefinition<?> p) {
			p.setCardinality(getCardinality());
			p.setDescription(getDescription());
			p.setDisplayName(getDisplayName());
			p.setId(getId());
			p.setIsInherited(isInherited());
			p.setIsOpenChoice(isOpenChoice());
			p.setIsOrderable(isOrderable());
			p.setIsQueryable(isQueryable());
			p.setIsRequired(isRequired());
			p.setLocalName(getLocalName());
			p.setLocalNamespace(getLocalNamespace());
			p.setPropertyType(getPropertyType());
			p.setQueryName(getQueryName());
			p.setUpdatability(getUpdatability());
		}

	}

	private static final BigInteger[] BIG_INTEGERS = {
		BigInteger.ZERO, BigInteger.ONE, BigInteger.TEN
	};
	private static final BigDecimal[] BIG_DECIMALS = {
		BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN
	};

	private void populateSpecificVariations(MPD<?> mpd, Consumer<MutablePropertyDefinition<?>> consumer) {
		switch (mpd.getPropertyType()) {
			case INTEGER:
				for (BigInteger min : TypeDataCodecTest.BIG_INTEGERS) {
					for (BigInteger max : TypeDataCodecTest.BIG_INTEGERS) {
						PropertyIntegerDefinitionImpl impl = PropertyIntegerDefinitionImpl.class
							.cast(TypeDataCodec.constructDefinition(mpd.getPropertyType()));
						mpd.copyTo(impl);
						impl.setMinValue(min);
						impl.setMaxValue(max);
						consumer.accept(impl);
					}
				}
				return;

			case DECIMAL:
				for (BigDecimal min : TypeDataCodecTest.BIG_DECIMALS) {
					for (BigDecimal max : TypeDataCodecTest.BIG_DECIMALS) {
						for (DecimalPrecision precision : DecimalPrecision.values()) {
							PropertyDecimalDefinitionImpl impl = PropertyDecimalDefinitionImpl.class
								.cast(TypeDataCodec.constructDefinition(mpd.getPropertyType()));
							mpd.copyTo(impl);
							impl.setMinValue(min);
							impl.setMaxValue(max);
							impl.setPrecision(precision);
							consumer.accept(impl);
						}
					}
				}
				return;

			case DATETIME:
				for (DateTimeResolution resolution : DateTimeResolution.values()) {
					PropertyDateTimeDefinitionImpl impl = PropertyDateTimeDefinitionImpl.class
						.cast(TypeDataCodec.constructDefinition(mpd.getPropertyType()));
					mpd.copyTo(impl);
					impl.setDateTimeResolution(resolution);
					consumer.accept(impl);
				}
				return;

			case STRING:
				for (BigInteger maxLen : TypeDataCodecTest.BIG_INTEGERS) {
					PropertyStringDefinitionImpl impl = PropertyStringDefinitionImpl.class
						.cast(TypeDataCodec.constructDefinition(mpd.getPropertyType()));
					mpd.copyTo(impl);
					impl.setMaxLength(maxLen);
					consumer.accept(impl);
				}
				break;

			default:
				MutablePropertyDefinition<?> def = TypeDataCodec.constructDefinition(mpd.getPropertyType());
				mpd.copyTo(def);
				consumer.accept(def);
				return;
		}
	}

	@Test
	public void testEncodeProperty() throws Exception {
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
																MPD<?> pd = new MPD<>();
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
																populateSpecificVariations(pd, this::verifyValidity);
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

		switch (original.getPropertyType()) {
			case INTEGER: {
				PropertyIntegerDefinitionImpl originalImpl = PropertyIntegerDefinitionImpl.class.cast(original);
				PropertyIntegerDefinitionImpl decodedImpl = PropertyIntegerDefinitionImpl.class.cast(decoded);
				Assertions.assertEquals(originalImpl.getMinValue(), decodedImpl.getMinValue());
				Assertions.assertEquals(originalImpl.getMaxValue(), decodedImpl.getMaxValue());
				break;
			}

			case DECIMAL: {
				PropertyDecimalDefinitionImpl originalImpl = PropertyDecimalDefinitionImpl.class.cast(original);
				PropertyDecimalDefinitionImpl decodedImpl = PropertyDecimalDefinitionImpl.class.cast(decoded);
				Assertions.assertEquals(originalImpl.getMinValue(), decodedImpl.getMinValue());
				Assertions.assertEquals(originalImpl.getMaxValue(), decodedImpl.getMaxValue());
				Assertions.assertEquals(originalImpl.getPrecision(), decodedImpl.getPrecision());
				break;
			}

			case DATETIME: {
				PropertyDateTimeDefinitionImpl originalImpl = PropertyDateTimeDefinitionImpl.class.cast(original);
				PropertyDateTimeDefinitionImpl decodedImpl = PropertyDateTimeDefinitionImpl.class.cast(decoded);
				Assertions.assertEquals(originalImpl.getDateTimeResolution(), decodedImpl.getDateTimeResolution());
				break;
			}

			case STRING: {
				PropertyStringDefinitionImpl originalImpl = PropertyStringDefinitionImpl.class.cast(original);
				PropertyStringDefinitionImpl decodedImpl = PropertyStringDefinitionImpl.class.cast(decoded);
				Assertions.assertEquals(originalImpl.getMaxLength(), decodedImpl.getMaxLength());
				break;
			}

			default:
				break;
		}

	}

	@Test
	public void testEncodeChoice() throws Exception {
	}

}
