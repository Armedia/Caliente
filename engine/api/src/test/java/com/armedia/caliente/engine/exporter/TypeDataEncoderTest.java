package com.armedia.caliente.engine.exporter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.chemistry.opencmis.commons.definitions.Choice;
import org.apache.chemistry.opencmis.commons.definitions.MutablePropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.DateTimeResolution;
import org.apache.chemistry.opencmis.commons.enums.DecimalPrecision;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AbstractPropertyDefinition;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ChoiceImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerDefinitionImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringDefinitionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class TypeDataEncoderTest {

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

	private <T> List<Choice<T>> renderChoices(PropertyType type, int maxWidth, int currentDepth) {
		List<Choice<T>> l = new ArrayList<>();
		for (int current = 0; current < maxWidth; current++) {
			ChoiceImpl<T> impl = new ChoiceImpl<>();

			impl.setDisplayName(String.format("%s-choice-%d-%d", type.value(), current, currentDepth));
			impl.setValue(renderDefaultValues(type, maxWidth));

			if (currentDepth < maxWidth) {
				for (int child = 0; child < maxWidth; child++) {
					impl.setChoice(renderChoices(type, maxWidth, currentDepth + 1));
				}
			}

			l.add(impl);
		}
		return l;
	}

	private <T> List<Choice<T>> renderChoices(PropertyType type, int width) {
		return renderChoices(type, width, 0);
	}

	private <T> List<T> renderDefaultValues(PropertyType type, final int i) {
		List<Object> values = new ArrayList<>(i);
		if (i > 0) {
			switch (type) {
				case BOOLEAN:
					for (int j = 0; j < i; j++) {
						values.add((j % 2) == 1);
					}
					break;

				case INTEGER:
					for (int j = 0; j < i; j++) {
						values.add(BigInteger.valueOf((j * 10) + 1));
					}
					break;

				case DECIMAL:
					for (int j = 0; j < i; j++) {
						values.add(BigDecimal.valueOf((j * 10) + 1));
					}
					break;

				case DATETIME:
					ZonedDateTime zdt = ZonedDateTime.now();
					for (int j = 0; j < i; j++) {
						values.add(GregorianCalendar.from(zdt.plus(Duration.ofDays(j))));
					}
					break;

				case ID:
				case URI:
				case HTML:
				case STRING:
					for (int j = 0; j < i; j++) {
						values.add(String.format("default-%s-value-%d", type.value(), j));
					}
					break;

			}
		}

		@SuppressWarnings("unchecked")
		List<T> defaultValues = (List<T>) values;
		return defaultValues;
	}

	private void populateSpecificVariations(MPD<?> mpd, Consumer<MutablePropertyDefinition<?>> consumer) {
		final PropertyType type = mpd.getPropertyType();
		switch (type) {
			case INTEGER:
				for (BigInteger min : TypeDataEncoderTest.BIG_INTEGERS) {
					for (BigInteger max : TypeDataEncoderTest.BIG_INTEGERS) {
						for (int i = 0; i < 3; i++) {
							PropertyIntegerDefinitionImpl impl = PropertyIntegerDefinitionImpl.class
								.cast(TypeDataEncoder.constructDefinition(mpd.getPropertyType()));
							mpd.copyTo(impl);
							impl.setMinValue(min);
							impl.setMaxValue(max);

							impl.setDefaultValue(renderDefaultValues(type, i));
							impl.setChoices(renderChoices(mpd.getPropertyType(), i));

							// Return the value
							consumer.accept(impl);
						}
					}
				}
				return;

			case DECIMAL:
				for (BigDecimal min : TypeDataEncoderTest.BIG_DECIMALS) {
					for (BigDecimal max : TypeDataEncoderTest.BIG_DECIMALS) {
						for (DecimalPrecision precision : DecimalPrecision.values()) {
							for (int i = 0; i < 3; i++) {
								PropertyDecimalDefinitionImpl impl = PropertyDecimalDefinitionImpl.class
									.cast(TypeDataEncoder.constructDefinition(mpd.getPropertyType()));
								mpd.copyTo(impl);
								impl.setMinValue(min);
								impl.setMaxValue(max);
								impl.setPrecision(precision);

								impl.setDefaultValue(renderDefaultValues(type, i));
								impl.setChoices(renderChoices(mpd.getPropertyType(), i));

								// Return the value
								consumer.accept(impl);
							}
						}
					}
				}
				return;

			case DATETIME:
				for (DateTimeResolution resolution : DateTimeResolution.values()) {
					for (int i = 0; i < 3; i++) {
						PropertyDateTimeDefinitionImpl impl = PropertyDateTimeDefinitionImpl.class
							.cast(TypeDataEncoder.constructDefinition(mpd.getPropertyType()));
						mpd.copyTo(impl);
						impl.setDateTimeResolution(resolution);

						impl.setDefaultValue(renderDefaultValues(type, i));
						impl.setChoices(renderChoices(mpd.getPropertyType(), i));

						// Return the value
						consumer.accept(impl);
					}
				}
				return;

			case STRING:
				for (BigInteger maxLen : TypeDataEncoderTest.BIG_INTEGERS) {
					for (int i = 0; i < 3; i++) {
						PropertyStringDefinitionImpl impl = PropertyStringDefinitionImpl.class
							.cast(TypeDataEncoder.constructDefinition(mpd.getPropertyType()));
						mpd.copyTo(impl);
						impl.setMaxLength(maxLen);

						impl.setDefaultValue(renderDefaultValues(type, i));
						impl.setChoices(renderChoices(mpd.getPropertyType(), i));

						// Return the value
						consumer.accept(impl);
					}
				}
				break;

			default:
				for (int i = 0; i < 3; i++) {
					MutablePropertyDefinition<?> def = TypeDataEncoder.constructDefinition(mpd.getPropertyType());
					mpd.copyTo(def);

					def.setDefaultValue(renderDefaultValues(type, i));
					def.setChoices(renderChoices(mpd.getPropertyType(), i));

					// Return the value
					consumer.accept(def);
				}
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

	private <V> void verifyValidity(PropertyDefinition<V> original) {
		Assertions.assertNotNull(original);

		PropertyDefinition<V> decoded = null;
		String encoded = null;
		try {
			encoded = TypeDataEncoder.encodeProperty(original);
			Assertions.assertNotNull(encoded);
			decoded = TypeDataEncoder.decodeProperty(encoded);
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
		Assertions.assertEquals(original.getDefaultValue(), decoded.getDefaultValue());
		validateChoices(original.getChoices(), decoded.getChoices());

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

	private <V> void validateChoices(List<Choice<V>> expected, List<Choice<V>> actual) {
		Assertions.assertFalse((expected == null) != (actual == null));
		Assertions.assertNotSame(expected, actual);
		Assertions.assertEquals(expected.size(), actual.size(), String.format("%s vs %s", expected, actual));

		final int total = expected.size();
		for (int i = 0; i < total; i++) {
			Choice<V> e = expected.get(i);
			Choice<V> a = actual.get(i);
			validateChoice(e, a);
		}
	}

	private <V> void validateChoice(Choice<V> expected, Choice<V> actual) {
		Assertions.assertFalse((expected == null) != (actual == null));
		Assertions.assertNotSame(expected, actual);

		Assertions.assertEquals(expected.getDisplayName(), actual.getDisplayName());
		Assertions.assertEquals(expected.getValue(), actual.getValue());
		validateChoices(expected.getChoice(), actual.getChoice());
	}

}